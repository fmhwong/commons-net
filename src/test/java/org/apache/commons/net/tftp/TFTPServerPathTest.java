/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.net.tftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.net.tftp.TFTPServer.ServerMode;

import junit.framework.TestCase;

/**
 * Some basic tests to ensure that the TFTP Server is honoring its read/write mode, and preventing files from being read or written from outside of the assigned
 * roots.
 */
public class TFTPServerPathTest extends TestCase {
    private static final int SERVER_PORT = 6901;
    String filePrefix = "tftp-";
    File serverDirectory = new File(System.getProperty("java.io.tmpdir"));

    public void testReadOnly() throws IOException {
        // Start a read-only server
        final TFTPServer tftpS = new TFTPServer(serverDirectory, serverDirectory, SERVER_PORT, ServerMode.GET_ONLY, null, null);

        // Create our TFTP instance to handle the file transfer.
        final TFTPClient tftp = new TFTPClient();
        tftp.open();
        tftp.setSoTimeout(2000);

        // make a file to work with.
        final File file = new File(serverDirectory, filePrefix + "source.txt");
        file.createNewFile();

        // Read the file from the tftp server.
        final File out = new File(serverDirectory, filePrefix + "out");

        // cleanup old failed runs
        out.delete();
        assertFalse("Couldn't clear output location", out.exists());

        try (final FileOutputStream output = new FileOutputStream(out)) {
            tftp.receiveFile(file.getName(), TFTP.BINARY_MODE, output, "localhost", SERVER_PORT);
        }

        assertTrue("file not created", out.exists());

        out.delete();

        try (final FileInputStream fis = new FileInputStream(file)) {
            tftp.sendFile(out.getName(), TFTP.BINARY_MODE, fis, "localhost", SERVER_PORT);
            fail("Server allowed write");
        } catch (final IOException e) {
            // expected path
        }
        file.delete();
        tftpS.shutdown();
    }

    public void testWriteOnly() throws IOException {
        // Start a write-only server
        final TFTPServer tftpS = new TFTPServer(serverDirectory, serverDirectory, SERVER_PORT, ServerMode.PUT_ONLY, null, null);

        // Create our TFTP instance to handle the file transfer.
        final TFTPClient tftp = new TFTPClient();
        tftp.open();
        tftp.setSoTimeout(2000);

        // make a file to work with.
        final File file = new File(serverDirectory, filePrefix + "source.txt");
        file.createNewFile();

        final File out = new File(serverDirectory, filePrefix + "out");

        // cleanup old failed runs
        out.delete();
        assertFalse("Couldn't clear output location", out.exists());

        try (final FileOutputStream output = new FileOutputStream(out)) {
            tftp.receiveFile(file.getName(), TFTP.BINARY_MODE, output, "localhost", SERVER_PORT);
            fail("Server allowed read");
        } catch (final IOException e) {
            // expected path
        }
        out.delete();

        try (final FileInputStream fis = new FileInputStream(file)) {
            tftp.sendFile(out.getName(), TFTP.BINARY_MODE, fis, "localhost", SERVER_PORT);
        }

        assertTrue("file not created", out.exists());

        // cleanup
        file.delete();
        out.delete();
        tftpS.shutdown();
    }

    public void testWriteOutsideHome() throws IOException {
        // Start a server
        final TFTPServer tftpS = new TFTPServer(serverDirectory, serverDirectory, SERVER_PORT, ServerMode.GET_AND_PUT, null, null);

        // Create our TFTP instance to handle the file transfer.
        final TFTPClient tftp = new TFTPClient();
        tftp.open();

        final File file = new File(serverDirectory, filePrefix + "source.txt");
        file.createNewFile();

        assertFalse("test construction error", new File(serverDirectory, "../foo").exists());

        try (final FileInputStream fis = new FileInputStream(file)) {
            tftp.sendFile("../foo", TFTP.BINARY_MODE, fis, "localhost", SERVER_PORT);
            fail("Server allowed write!");
        } catch (final IOException e) {
            // expected path
        }

        assertFalse("file created when it should not have been", new File(serverDirectory, "../foo").exists());

        // cleanup
        file.delete();

        tftpS.shutdown();
    }

}
