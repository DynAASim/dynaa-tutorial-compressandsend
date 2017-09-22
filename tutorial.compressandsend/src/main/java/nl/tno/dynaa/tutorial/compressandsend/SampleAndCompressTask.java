/**
 * File SampleAndCompressTask.java
 *
 * Copyright 2017 TNO
 */
package nl.tno.dynaa.tutorial.compressandsend;

import java.util.Random;

import javax.measure.Quantity;
import javax.measure.quantity.Time;

import nl.tno.dynaa.model.functional.Message;
import nl.tno.dynaa.model.functional.Task;
import nl.tno.dynaa.model.functional.Task.OutputPort;
import nl.tno.dynaa.model.functional.TaskContext;
import nl.tno.dynaa.model.functional.behavioural.CalculateSegment;
import nl.tno.dynaa.model.functional.behavioural.CopyDataSegment;
import nl.tno.dynaa.model.functional.behavioural.DelaySegment;
import nl.tno.dynaa.model.functional.behavioural.LinkedBehaviourSpecification;
import nl.tno.dynaa.model.functional.behavioural.SendSegment;
import nl.tno.dynaa.model.functional.behavioural.TaskSegment;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.Units;

/**
 * SampleAndCompressNode
 *
 * This class contains part of a functional view in the DynAA model.
 *
 * The DynAA "model" layer provides a basic template of a generic computational task. This template lays down a base for
 * describing (computational) behaviour (Processes, SW, algorithms, etc.)
 *
 * A user (designer) may fill in this template with information about:
 *
 * -- A behaviour for the task -- what it does, or a model of how computation works in it
 *
 * -- Meta-information about the computation, such as computational effort (number of floating operations per second,
 * number of integer operations per second)
 *
 * -- Task parameters
 *
 * -- Communication between tasks -- Tasks can communicate with each other. For that, they use ports and connections.
 * Transmission of messages through these connections occur using a process network communication semantic.
 *
 * In this file, we custom model the typical behaviour of a sensor task of sampling and sending the data. Additionally,
 * we add the possibility of compressing the data before sending. Compression can be done by using a 'ZIP' algorithm or
 * a 'RAR' algorithm (fictive), each one of them requiring a different computational effort dependent on the compression
 * rate.
 *
 */
public class SampleAndCompressTask {

    /**
     * Every task maintains a "context". The context is like a memory for local variables (to the task).
     *
     * Like a memory, values in the context is accessed by a "key" or variable name.
     */
    public static final String SENSOR_DATA_CONTEXT_KEY = "SENSOR_DATA";
    public static final String COMPRESS_DATA_CONTEXT_KEY = "COMPRESS_DATA";

    public static Task build() throws Exception {

        /**
         * The construction of a task begins by creating an empty behaviour and a task object.
         *
         * The behaviour is associated to the task.
         */
        final LinkedBehaviourSpecification taskBehaviour = new LinkedBehaviourSpecification();
        final Task sampleAndCompressTask = new Task(taskBehaviour);

        /**
         * We also add ports to the task. Ports are used by the tasks to send messages to their external environment and
         * other tasks.
         */
        final OutputPort outputPort = sampleAndCompressTask.addOutputPort("OUTPORT");

        /**
         * It is possible to add properties to a task.
         *
         * Properties are sort of parameters that can be freely accessed and modified by the user. They may, for
         * example, select the algorithm to be used in the task, adjust computational load associated to operations,
         * etc...
         *
         */
        sampleAndCompressTask.addProperty("AVERAGE_PACKAGE_SIZE", (double) 5 * (2 ^ 20));
        sampleAndCompressTask.addProperty("SDEVIATION_PACKAGE_SIZE", 0.0);

        sampleAndCompressTask.addProperty("COMPRESSION_ALGORITHM", "NONE"); // "ZIP", "RAR"
        sampleAndCompressTask.addProperty("COMPRESSION_PERCENTAGE", 0.0); // [0.0, 100.0]

        /**
         * Further on, the construction of a task consists in defining its behavior. There are many ways to do that, but
         * in here we are going to show how that can be done using segments.
         *
         * Segments are building blocks of a more complex behaviour (like functions, or activities in an activity
         * flowchart).
         *
         * Commonly used segments are available in the DynAA library to ease the modelling process. Example of available
         * segments are:
         *
         * -- Delay segments -> used to model a delay in the task (but without computation involved -- idle, waiting).
         * -- Calculate segments -> used to model a computational effort by the processor -- requires an indication of
         * the computational load in terms of the number of floating operations and integer operations required.
         * -- Data copy segments -> more an utility segment behavior to ease the transfer of context information between
         * segments.
         * -- Send segments -> used to send messages through a port to another task
         * -- Receive segments -> used to receive messages from a port.
         *
         * We are going as well to write our own custom segments !
         */
        final Quantity<Time> delayTime = Quantities.getQuantity(5.0, Units.SECOND);
        final DelaySegment delaySegment = new DelaySegment(delayTime);

        // sample segment
        final TaskSegment senseSegment = SampleAndCompressTask.createSenseSegment(sampleAndCompressTask);

        // execute sample calculations
        final CalculateSegment calculateSensorSampling = new CalculateSegment();

        // move data from sample to compress context
        final CopyDataSegment sense2compress = new CopyDataSegment(SampleAndCompressTask.SENSOR_DATA_CONTEXT_KEY,
                SampleAndCompressTask.COMPRESS_DATA_CONTEXT_KEY);

        // compress segment
        final TaskSegment compress = SampleAndCompressTask.createCompressSegment(sampleAndCompressTask);

        // move data from compress to send context
        final CopyDataSegment compress2send = new CopyDataSegment(SampleAndCompressTask.COMPRESS_DATA_CONTEXT_KEY,
                SendSegment.MESSAGE_SEND_CONTEXT_KEY);

        final CalculateSegment calculateCompression = new CalculateSegment();

        final SendSegment send = new SendSegment(outputPort, null, false);

        /**
         * Segments are then organized in a control flow (here, a simple loop).
         *
         * The control flow between segments is registered in the task behaviour object.
         *
         */
        taskBehaviour.setLooping(true);
        taskBehaviour.addSegment(delaySegment);
        taskBehaviour.addSegment(senseSegment);
        taskBehaviour.addSegment(calculateSensorSampling);
        taskBehaviour.addSegment(sense2compress);
        taskBehaviour.addSegment(compress);
        taskBehaviour.addSegment(calculateCompression);
        taskBehaviour.addSegment(compress2send);
        taskBehaviour.addSegment(send);

        return sampleAndCompressTask;

    }

    /**
     * Creates a custom task segment that models the compression algorithm computational effort.
     *
     * @param sampleAndCompressTask The task associated to this segment.
     * @return a new task segment.
     */
    private static TaskSegment createCompressSegment(final Task sampleAndCompressTask) {
        return new TaskSegment() {

            @Override
            public void run(final TaskContext taskContext) throws Exception {

                // compress parameters based on algorithm and rate
                final String algorithm = (String) sampleAndCompressTask.get("COMPRESSION_ALGORITHM");
                final double compressionRate = (double) sampleAndCompressTask.get("COMPRESSION_PERCENTAGE");

                final double dataSize = (double) taskContext.get(SampleAndCompressTask.COMPRESS_DATA_CONTEXT_KEY);

                int packetSize = (int) (2.0 * dataSize);

                int nOperations = 1;

                switch (algorithm) {
                case "ZIP":
                    packetSize = (int) (SampleAndCompressTask.zipSize(compressionRate) * dataSize);
                    nOperations = (int) (SampleAndCompressTask.zipFlOps(compressionRate) * dataSize);
                    break;
                case "RAR":
                    packetSize = (int) (SampleAndCompressTask.rarSize(compressionRate) * dataSize);
                    nOperations = (int) (SampleAndCompressTask.rarFlOps(compressionRate) * dataSize);
                    break;
                case "NONE":
                    break;
                default:
                    if (!(algorithm.equals("NONE"))) {
                        System.out.println("Unknown compression method: '" + algorithm + "': no compression applied.");
                    }
                    break;
                }

                taskContext.put(CalculateSegment.FLOPS_CONTEXT_KEY, nOperations);
                taskContext.put(CalculateSegment.IOPS_CONTEXT_KEY, 0);

                // prepare message to send
                final Message msg = Message.createFrom(packetSize, null, null);
                taskContext.put(SampleAndCompressTask.COMPRESS_DATA_CONTEXT_KEY, msg);
                this.scheduleNow(TaskSegment.SEGMENT_SUCCESS);
            }
        };
    }

    /**
     * Creates a custom task segment that models the computational effort of sampling a new data.
     *
     * @param sampleAndCompressTask The task associated to this segment.
     * @return a new task segment.
     */
    private static TaskSegment createSenseSegment(final Task sampleAndCompressTask) {
        return new TaskSegment() {

            @Override
            public void run(final TaskContext taskContext) throws Exception {

                // generate packet of certain size
                final double mu = (double) sampleAndCompressTask.get("AVERAGE_PACKAGE_SIZE");
                final double sigma = (double) sampleAndCompressTask.get("SDEVIATION_PACKAGE_SIZE");

                final Random r = new Random();
                final double dataSize = mu + (sigma * r.nextGaussian());

                // prepare calculation
                taskContext.put(CalculateSegment.FLOPS_CONTEXT_KEY, (int) (dataSize));
                taskContext.put(CalculateSegment.IOPS_CONTEXT_KEY, 0);

                // set data
                taskContext.put(SampleAndCompressTask.SENSOR_DATA_CONTEXT_KEY, dataSize);
                this.scheduleNow(TaskSegment.SEGMENT_SUCCESS);
            }
        };
    }

    /**
     * zipSize determines the final size of a file, when the "ZIP" algorithm is used targeting a certain
     * compression percentage.
     *
     * @param percentage the target compression rate when using the "ZIP" compression algorithm
     * @return the average compression rate after compression.
     */
    static double zipSize(final double percentage) {
        return 0.978 + (1.01 * Math.cos((Math.PI * (percentage + 120)) / (240)));
    }

    /**
     * zipFlOps models the average number of floating point operations performed when executing the "ZIP" compression
     * algorithm and targeting a given compression rate.
     *
     * @param percentage the target compression rate.
     * @return the average number of floating point operations performed.
     */
    static double zipFlOps(final double percentage) {
        final double scale = 1.0e3;
        final double bumpyPart = Math.cos(Math.pow(percentage / 40.0, 1.6));
        return scale * ((1 / Math.pow(101 - percentage, 0.45)) + bumpyPart);
    }

    /**
     * rarSize determines the final size of a file, when the "RAR" algorithm is used targeting a certain
     * compression percentage.
     *
     * @param percentage the target compression rate when using the "RAR" compression algorithm
     * @return the average compression rate after compression.
     */
    static double rarSize(final double percentage) {
        return (3 / ((percentage + 61.0) / 32.0)) - .596;
    }

    /**
     * rarFlOps models the average number of floating point operations performed when executing the "RAR" compression
     * algorithm and targeting a given compression rate.
     *
     * @param percentage the target compression rate.
     * @return the average number of floating point operations performed.
     */
    static double rarFlOps(final double percentage) {
        final double scale = 1.0e3;
        final double bumpyPart = Math.sin(Math.pow(percentage / 45.0, 1.3));
        return scale * ((1 / Math.pow(102 - percentage, 0.39)) + bumpyPart);
    }

}
