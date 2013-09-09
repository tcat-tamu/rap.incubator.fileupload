/*******************************************************************************
 * Copyright (c) 2013 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.addons.fileupload.internal;

import static org.eclipse.rap.addons.fileupload.test.FileUploadTestUtil.fakeUploadRequest;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rap.addons.fileupload.FileDetails;
import org.eclipse.rap.addons.fileupload.FileUploadEvent;
import org.eclipse.rap.addons.fileupload.FileUploadHandler;
import org.eclipse.rap.addons.fileupload.FileUploadReceiver;
import org.eclipse.rap.addons.fileupload.test.FileUploadTestUtil.FileData;
import org.eclipse.rap.addons.fileupload.test.TestFileUploadListener;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.TestResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


public class FileUploadProcessor_Test {

  private TestFileUploadListener testListener;
  private FileUploadReceiver receiver;
  private FileUploadHandler uploadHandler;
  private FileUploadProcessor uploadProcessor;

  @Before
  public void setUp() {
    Fixture.setUp();
    testListener = new TestFileUploadListener();
    receiver = mock( FileUploadReceiver.class );
    uploadHandler = new FileUploadHandler( receiver );
    uploadProcessor = new FileUploadProcessor( uploadHandler );
  }

  @After
  public void tearDown() {
    uploadHandler.dispose();
    Fixture.tearDown();
  }

  @Test
  public void testHandleFileUpload_singleFile_notifiesListener() throws IOException {
    uploadHandler.addUploadListener( testListener );

    fakeUploadRequest( uploadHandler, "foo", "text/plain", "foo.txt" );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    assertEquals( "progress.finished.", testListener.getLog() );
  }

  @Test
  public void testHandleFileUpload_singleFile_setsFileUploadEvent() throws IOException {
    uploadHandler.addUploadListener( testListener );

    fakeUploadRequest( uploadHandler, "foo", "text/plain", "foo.txt" );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    FileUploadEvent event = testListener.getLastEvent();
    FileDetails[] fileDetails = event.getFileDetails();
    assertEquals( "foo.txt", fileDetails[ 0 ].getFileName() );
    assertEquals( "text/plain", fileDetails[ 0 ].getContentType() );
    assertEquals( 3, fileDetails[ 0 ].getContentLength() );
    assertEquals( event.getContentLength(), event.getBytesRead() );
  }

  @Test
  public void testHandleFileUpload_singleFile_callsReciever() throws IOException {
    fakeUploadRequest( uploadHandler, "foo", "text/plain", "foo.txt" );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    ArgumentCaptor<FileDetails> captor = ArgumentCaptor.forClass( FileDetails.class );
    verify( receiver ).receive( any( InputStream.class ), captor.capture() );
    FileDetails uploadDetails = captor.getValue();
    assertEquals( 3, uploadDetails.getContentLength() );
    assertEquals( "text/plain", uploadDetails.getContentType() );
    assertEquals( "foo.txt", uploadDetails.getFileName() );
  }

  @Test
  public void testHandleFileUpload_multipleFiles_notifiesListener() throws IOException {
    uploadHandler.addUploadListener( testListener );

    FileData file1 = new FileData( "foo", "text/plain", "foo.txt" );
    FileData file2 = new FileData( "bar", "image/png", "bar.png" );
    fakeUploadRequest( uploadHandler, file1, file2 );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    assertEquals( "progress.finished.", testListener.getLog() );
  }

  @Test
  public void testHandleFileUpload_multipleFiles_setsFileUploadEvent() throws IOException {
    uploadHandler.addUploadListener( testListener );

    FileData file1 = new FileData( "foo", "text/plain", "foo.txt" );
    FileData file2 = new FileData( "bar", "image/png", "bar.png" );
    fakeUploadRequest( uploadHandler, file1, file2 );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    FileUploadEvent event = testListener.getLastEvent();
    FileDetails[] fileDetails = event.getFileDetails();
    assertEquals( "foo.txt", fileDetails[ 0 ].getFileName() );
    assertEquals( "text/plain", fileDetails[ 0 ].getContentType() );
    assertEquals( 3, fileDetails[ 0 ].getContentLength() );
    assertEquals( "bar.png", fileDetails[ 1 ].getFileName() );
    assertEquals( "image/png", fileDetails[ 1 ].getContentType() );
    assertEquals( 3, fileDetails[ 1 ].getContentLength() );
    assertEquals( event.getContentLength(), event.getBytesRead() );
  }

  @Test
  public void testHandleFileUpload_multipleFiles_callsReciever() throws IOException {
    FileData file1 = new FileData( "foo", "text/plain", "foo.txt" );
    FileData file2 = new FileData( "bar", "image/png", "bar.png" );
    fakeUploadRequest( uploadHandler, file1, file2 );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    ArgumentCaptor<FileDetails> captor = ArgumentCaptor.forClass( FileDetails.class );
    verify( receiver, times( 2 ) ).receive( any( InputStream.class ), captor.capture() );
    List<FileDetails> values = captor.getAllValues();
    assertEquals( 3, values.get( 0 ).getContentLength() );
    assertEquals( "text/plain", values.get( 0 ).getContentType() );
    assertEquals( "foo.txt", values.get( 0 ).getFileName() );
    assertEquals( 3, values.get( 1 ).getContentLength() );
    assertEquals( "image/png", values.get( 1 ).getContentType() );
    assertEquals( "bar.png", values.get( 1 ).getFileName() );
  }

  @Test
  public void testHandleFileUpload_withoutData() throws IOException {
    uploadHandler.addUploadListener( testListener );

    fakeUploadRequest( uploadHandler );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    assertEquals( "progress.failed.", testListener.getLog() );
    assertEquals( HttpServletResponse.SC_BAD_REQUEST, getResponseErrorStatus() );
  }

  @Test
  public void testHandleFileUpload_fileExceedsMaxSize() throws IOException {
    uploadHandler.setMaxFileSize( 5 );
    uploadHandler.addUploadListener( testListener );

    FileData file1 = new FileData( "foo", "text/plain", "foo.txt" );
    FileData file2 = new FileData( "bar bar", "image/png", "bar.png" );
    fakeUploadRequest( uploadHandler, file1, file2 );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    assertEquals( "progress.failed.", testListener.getLog() );
    assertEquals( HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, getResponseErrorStatus() );
  }

  @Test
  public void testHandleFileUpload_withExceptionInReciever() throws IOException {
    doThrow( new IOException() ).when( receiver ).receive( any( InputStream.class ),
                                                           any( FileDetails.class ) );
    uploadHandler.addUploadListener( testListener );

    fakeUploadRequest( uploadHandler, "foo", "text/plain", "foo.txt" );
    uploadProcessor.handleFileUpload( RWT.getRequest(), RWT.getResponse() );

    assertEquals( "progress.failed.", testListener.getLog() );
    assertEquals( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, getResponseErrorStatus() );
  }

  private static int getResponseErrorStatus() {
    TestResponse response = ( TestResponse )RWT.getResponse();
    return response.getErrorStatus();
  }

}
