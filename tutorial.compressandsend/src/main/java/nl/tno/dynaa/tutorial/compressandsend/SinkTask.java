/**
 * File SinkTask.java
 *
 * Copyright 2017 TNO
 */
package nl.tno.dynaa.tutorial.compressandsend;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import nl.tno.dynaa.core.engine.SimulationEngine;
import nl.tno.dynaa.model.functional.Message;
import nl.tno.dynaa.model.functional.Task;
import nl.tno.dynaa.model.functional.Task.InputPort;
import nl.tno.dynaa.model.functional.TaskContext;
import nl.tno.dynaa.model.functional.behavioural.DelaySegment;
import nl.tno.dynaa.model.functional.behavioural.LinkedBehaviourSpecification;
import nl.tno.dynaa.model.functional.behavioural.ReceiveSegment;
import nl.tno.dynaa.model.functional.behavioural.TaskSegment;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.Units;

/**
 * SinkTask
 *
 * The SinkTask is used to receive messages, print their content, and discard the message(sink).
 *
 * The SinkTask is blocking when receiving the next message.
 *
 */
public class SinkTask {

    public static Task build() throws Exception {

        final LinkedBehaviourSpecification sinkBehaviour = new LinkedBehaviourSpecification();
        final Task aTask = new Task(sinkBehaviour);
        final InputPort inPort = aTask.addInputPort("INPORT");

        sinkBehaviour.setLooping(true);
        sinkBehaviour.addSegment(new ReceiveSegment(inPort, null, true));
        sinkBehaviour.addSegment(SinkTask.printMsgSegment());
        final Quantity<Time> delay = Quantities.getQuantity(0.1, Units.SECOND);
        sinkBehaviour.addSegment(new DelaySegment(delay));

        return aTask;
    }

    /**
     * Builds an returns a print message task segment
     *
     * @return a task segment to print the received messages.
     */
    private static TaskSegment printMsgSegment() {
        final TaskSegment printMsgSegment = new TaskSegment() {

            @Override
            public void run(final TaskContext taskContext) throws Exception {
                final Message msg = (Message) taskContext.get(ReceiveSegment.MESSAGE_RECEIVED_CONTEXT_KEY);
                System.out.println("Message receive on Sink at " + SimulationEngine.getInstance().getCurrentTime()
                        + " with size " + msg.getField(Message.SIZE) + ". ");
                this.scheduleNow(TaskSegment.SEGMENT_SUCCESS);
            }
        };
        return printMsgSegment;
    }

}
