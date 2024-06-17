//-----------------------------------------------------------------------------
//
//	CustomEvents
//
//	Author:		Mike Smits
//	Date:		15 May 19
//	Revision:	2.0.190515.1941
//
//-----------------------------------------------------------------------------

package com.savvytech.savvylevelfour.common;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

public class CustomEvents {

    public CustomEvents() {}

    // MyEvent Class ----------------------------------------------------------

    public class MyEvent {

        public MyEvent() {
        }

        private List<MyEventListener> listeners = new
                ArrayList<MyEventListener>();

        public synchronized void addEventListener(MyEventListener listener) {
            listeners.add(listener);
        }

        public synchronized void removeEventListener(MyEventListener listener) {
            listeners.remove(listener);
        }

        public synchronized void fireEvent(char newValue) {
            MyEventClass event = new MyEventClass(this, newValue);
            Iterator<MyEventListener> i = listeners.iterator();
            while (i.hasNext()) {
                ((MyEventListener) i.next()).handleMyEventClassEvent(event);
            }
        }

    }

    // MyEvent Class ----------------------------------------------------------

    public class MyEventClass extends EventObject {

        public char newValue;

        public MyEventClass(Object source, char newValue) {
            super(source);
            this.newValue = newValue;
        }
    }


    // MyEventClassListener Interface -----------------------------------------

    public interface MyEventListener {
        public void handleMyEventClassEvent(MyEventClass e);
    }

}


