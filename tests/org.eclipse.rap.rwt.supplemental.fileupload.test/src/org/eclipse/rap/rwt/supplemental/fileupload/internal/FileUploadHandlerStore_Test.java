/*******************************************************************************
 * Copyright (c) 2011, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.rwt.supplemental.fileupload.internal;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.supplemental.fileupload.FileUploadHandler;
import org.eclipse.rap.rwt.supplemental.fileupload.test.TestFileUploadReceiver;
import org.eclipse.rap.rwt.testfixture.Fixture;


public class FileUploadHandlerStore_Test extends TestCase {

  private FileUploadHandlerStore itemStore;
  private FileUploadHandler testHandler;

  @Override
  protected void setUp() throws Exception {
    Fixture.setUp();
    itemStore = FileUploadHandlerStore.getInstance();
    testHandler = new FileUploadHandler( new TestFileUploadReceiver() );
  }

  @Override
  protected void tearDown() throws Exception {
    testHandler.dispose();
    testHandler = null;
    itemStore = null;
    Fixture.tearDown();
  }

  public void testGetInstance() {
    assertNotNull( itemStore );
    assertSame( itemStore, FileUploadHandlerStore.getInstance() );
  }

  public void testGetNotExistingHandler() {
    FileUploadHandler result = itemStore.getHandler( "testId" );

    assertNull( result );
  }

  public void testRegisterAndGetHandler() {
    itemStore.registerHandler( "testId", testHandler );

    FileUploadHandler result = itemStore.getHandler( "testId" );

    assertSame( testHandler, result );
  }

  public void testGetHandlerWithDifferentId() {
    itemStore.registerHandler( "testId", testHandler );

    FileUploadHandler result = itemStore.getHandler( "anotherId" );

    assertNull( result );
  }

  public void testDeregisterHandler() {
    itemStore.registerHandler( "testId", testHandler );

    itemStore.deregisterHandler( "testId" );
    FileUploadHandler result = itemStore.getHandler( "testId" );

    assertNull( result );
  }

  public void testCreateToken() {
    String token = FileUploadHandlerStore.createToken();

    assertNotNull( token );
    assertTrue( token.length() > 0 );
    assertFalse( token.equals( FileUploadHandlerStore.createToken() ) );
  }
}
