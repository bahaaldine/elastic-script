package org.elasticsearch.xpack.escript.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

/**
 * A simple in-memory appender that collects log messages.
 */
public class TestListAppender extends AbstractAppender {

    private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

    public TestListAppender(String name, Filter filter, Layout<?> layout) {
        super(name, filter, layout, false);
    }

    @Override
    public void append(LogEvent event) {
        String msg = event.getMessage().getFormattedMessage();
        System.out.println("TestListAppender captured: " + msg);
        messages.add(msg);
    }

    public List<String> getMessages() {
        return messages;
    }
}
