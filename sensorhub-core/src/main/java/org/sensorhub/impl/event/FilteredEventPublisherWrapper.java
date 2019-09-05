/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are copyright (C) 2019, Sensia Software LLC
 All Rights Reserved. This software is the property of Sensia Software LLC.
 It cannot be duplicated, used, or distributed without the express written
 consent of Sensia Software LLC.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.event;

import java.util.concurrent.Flow.Subscriber;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventPublisher;


/**
 * <p>
 * Publisher wrapper for filtering events using the provided predicate.<br/>
 * For instance, this is used to extract events from one or more sources from
 * a publisher group channel. 
 * </p>
 *
 * @author Alex Robin
 * @date Mar 22, 2019
 */
public class FilteredEventPublisherWrapper implements IEventPublisher
{
    String sourceID;
    IEventPublisher wrappedPublisher;
    Predicate<Event> filter;
    

    public FilteredEventPublisherWrapper(IEventPublisher wrappedPublisher, String sourceID, final Predicate<Event> filter)
    {
        this.sourceID = sourceID;
        this.wrappedPublisher = wrappedPublisher;
        this.filter = filter;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(Subscriber<? super Event> subscriber)
    {
        /*if (wrappedPublisher instanceof FilteredSubmissionPublisherV10)
            ((FilteredSubmissionPublisherV10<Event>)wrappedPublisher).subscribe(subscriber, filter);
        else if (wrappedPublisher instanceof FilteredSubmissionPublisherV11)
            ((FilteredSubmissionPublisherV11<Event>)wrappedPublisher).subscribe(subscriber, filter);
        else*/
            wrappedPublisher.subscribe(new FilteredSubscriber<Event>(subscriber, filter));
    }


    @Override
    public int getNumberOfSubscribers()
    {
        return wrappedPublisher.getNumberOfSubscribers();
    }


    @Override
    public void publish(Event e)
    {
        wrappedPublisher.publish(e);
    }


    @Override
    public void publish(Event e, BiPredicate<Subscriber<? super Event>, ? super Event> onDrop)
    {
        wrappedPublisher.publish(e, onDrop);
    }

}