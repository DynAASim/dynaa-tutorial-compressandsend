/**
 * File CompressAndSend.java
 *
 * Copyright 2017 TNO
 */
package nl.tno.dynaa.tutorial.compressandsend;

import nl.tno.dynaa.core.engine.SimulationEngine;
import nl.tno.dynaa.library.channels.DelayChannel;
import nl.tno.dynaa.library.logging.MessageCountLogger;
import nl.tno.dynaa.library.logging.NodePowerLogger;
import nl.tno.dynaa.model.ModelUtils;
import nl.tno.dynaa.model.functional.Task;
import nl.tno.dynaa.model.functional.Task.InputPort;
import nl.tno.dynaa.model.functional.Task.OutputPort;
import nl.tno.dynaa.model.physical.Channel;
import nl.tno.dynaa.model.physical.CommunicationDevice;
import nl.tno.dynaa.model.physical.Node;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.Units;

/**
 * CompressAndSend
 *
 * This class contains a DynAA model.
 *
 * The DynAA model example in this file represents a small system with two nodes.
 *
 * The first node is a sensor node. It models a sensor device that produces samples of the environment, and send the
 * sampled data to the receiver.
 *
 * The second node is a receiver node, that in this example only works as a sink of information. Meaning, no further
 * processing is done in this node.
 *
 * Each sample is represented by several hundred bytes (e.g. an image) to be sent. Sending
 * so much information each time is an expensive operation in terms of power. This is a problem as the sensor node is
 * powered by batteries.
 *
 * Because of that, the sensor node is
 * provided with several compressing algorithms that can be chosen by the user/designer. It is also possible to
 * determine the compression rate desired from the compression algorithm. Of course, compressing the data can be
 * computationally intensive and as such spends power.
 *
 * This model is done to study how the system performs :
 *
 * -- When changing among the different compression algorithms;
 *
 * -- When changing the compression rate at each compression algorithm;
 *
 * -- When changing the processor to be used in the sensor node;
 *
 * The criteria to be used is a trade-off between messages passed to the receiver per unit of time (possible throughput)
 * allowed by a given system. And the average energy consumption of the node along the time -- which influences its
 * operational lifetime.
 *
 */
public class CompressAndSend {

    public static void runExample() throws Exception {

        /**
         * The first part of a DynAA model is called the functional view.
         *
         * A functional view (or model) defines the tasks that are executed in the system. Tasks are models of the
         * computational behaviour in the system.
         *
         * Each task is modelled in a separate class. During the initialization, it is possible to set parameters of
         * these tasks. In our example file, we are going to adjust the compression rate, and the compression algorithm
         * to be used.
         *
         */
        final Task compressAndSendTask = SampleAndCompressTask.build();
        final Task sinkTask = SinkTask.build();

        final String compressionAlgorithm = "ZIP";
        final double compressionPercentage = 20.0;

        compressAndSendTask.set("COMPRESSION_ALGORITHM", compressionAlgorithm);
        compressAndSendTask.set("COMPRESSION_PERCENTAGE", compressionPercentage);

        /**
         * The second part of a DynAA model is called the physical view.
         *
         * A physical model defines which nodes (devices) are used and how they are connected.
         *
         * The physical model also defines other aspects of the physical reality, such as how the communication channel
         * behaves or physical dynamics of the environment (temperature, humidity, etc.).
         *
         * In this example, we will use a SampleAndCompressNode, which is a model of the sensor device. And a SinkNode,
         * a simple node used only as a sink for the messages.
         *
         * We will also use several channel models which affect the performance of the transmission and therefore the
         * amount of necessary energy used to transmit the messages.
         *
         */

        final Node compressAndSendNode = SampleAndCompressNode.build();
        final Node sinkNode = SinkNode.build();

        final CommunicationDevice compressAndSendCommDev = ((CommunicationDevice) compressAndSendNode
                .getPeripheral("COMM_DEVICE"));
        final CommunicationDevice sinkDev = ((CommunicationDevice) sinkNode.getPeripheral("COMM_DEVICE"));

        final Channel channel = new DelayChannel(5 * (2 ^ 20)); // 5 Mbytes per second bandwidth
        compressAndSendCommDev.setChannel(channel);
        sinkDev.setChannel(channel);
        sinkDev.listen();

        /**
         * The third part of a DynAA model is called the mapping view.
         *
         * The mapping view maps tasks into the physical nodes where they will be executed. The mapping view may contain
         * variations (not enabled in this example) in the mapping, which allows the designer to analyse the impact of
         * using different allocations to the tasks.
         *
         * The mapping view:
         *
         * -- assigns tasks to the physical nodes where they should be executed.
         *
         * -- binds task communication ports to the communication devices of a physical node. This way, you can quickly
         * try out two transmitting messages through two different radios, for example !
         *
         */
        compressAndSendNode.execute(compressAndSendTask);
        sinkNode.execute(sinkTask);

        compressAndSendCommDev.bind((OutputPort) compressAndSendTask.port("OUTPORT"));
        sinkDev.bind((InputPort) sinkTask.port("INPORT"));

        /**
         * Finally, often a DynAA model uses loggers to collect information during a simulation.
         *
         * Loggers are special elements similar to probe points. They listen to a certain variable of the system and log
         * changes in their values along the time.
         *
         * The data stored in a logger can be profiled later (processed) to extract more complex information about the
         * system and calculate key performance indicators (KPIs).
         *
         * In this example, we will activate a logger to count messages received in the SinkNode and a logger to monitor
         * the power consumed by a certain physical node.
         */

        final MessageCountLogger messageCountLogger = new MessageCountLogger((InputPort) sinkTask.port("INPORT"));
        final NodePowerLogger nodePowerLogger = new NodePowerLogger(compressAndSendNode);

        /**
         * We activate the simulation of a DynAA model, by invoking the method run of the simulation engine.
         *
         * Remember to assign a stop time for your simulation, otherwise it will run forever, or until there are no
         * events available in the system anymore.
         *
         */

        final double simulationSeconds = 100.0;
        SimulationEngine.getInstance()
                .run(ModelUtils.toCoreTime(Quantities.getQuantity(simulationSeconds, Units.SECOND)));

        /**
         * Simulation is done.
         *
         * We can now inspect the loggers.
         */
        System.out.println("Total number of messages received: " + messageCountLogger.getMessageCount());
        System.out.println(nodePowerLogger.getPowerLog().toString());
    }

    /**
     * Main procedure.
     *
     * Creates and invokes the compress and send example in this file.
     *
     */
    public static void main(final String[] args) {

        try {
            CompressAndSend.runExample();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}