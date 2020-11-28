/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.swe;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.servlet.WriteListener;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;


/**
 * <p>
 * Adapter output stream for sending data to a websocket.<br/>
 * Data is actually sent to the web socket only when flush() is called.
 * </p>
 *
 * @author Alex Robin
 * @since Feb 19, 2015
 */
public class WebSocketOutputStream extends ByteArrayOutputStream implements IAsyncOutputStream
{
    Logger log;
    ByteBuffer buffer;
    Session session;
    boolean autoSendOnFlush;
    boolean closed;
    
    
    public WebSocketOutputStream(Session session, int bufferSize)
    {
        this(session, bufferSize, true, null);
    }
    
    
    public WebSocketOutputStream(Session session, int bufferSize, boolean autoSendOnFlush, Logger log)
    {
        super(bufferSize);
        this.session = session;
        this.buffer = ByteBuffer.wrap(this.buf);
        this.autoSendOnFlush = autoSendOnFlush;
        this.log = log;
    }
    
    
    @Override
    public void close()
    {
        if (session.isOpen())
            WebSocketUtils.closeSession(session, StatusCode.NORMAL, "Data provider done", log);
        closed = true;
    }
    

    @Override
    public void flush() throws IOException
    {
        if (autoSendOnFlush)
            send();
    }
    
    
    public void send() throws IOException
    {
        if (closed)
            throw new EOFException();
        
        // do nothing if no more bytes have been written since last call
        if (count == 0)
            return;
        
        // detect when buffer has grown
        if (count > buffer.capacity())
            this.buffer = ByteBuffer.wrap(this.buf);
        
        buffer.limit(count);
        session.getRemote().sendBytes(buffer);
        //System.out.println("Sending " + count + " bytes");
        
        // reset so we can write again in same buffer
        this.reset();
        buffer.rewind();
    }
    
    
    @Override
    public boolean isClosed()
    {
        return !session.isOpen();
    }
    
    
    @Override
    public boolean isReady()
    {
        return true;
    }
    
    
    @Override
    public void setWriteListener(WriteListener listener)
    {
    }

}
