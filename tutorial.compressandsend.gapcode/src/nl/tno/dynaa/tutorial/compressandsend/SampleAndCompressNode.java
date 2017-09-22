/**
 * File SampleAndCompressNode.java
 *
 * Copyright 2017 TNO
 */
package nl.tno.dynaa.tutorial.compressandsend;

import java.util.HashMap;
import java.util.Map;

import javax.measure.Quantity;
import javax.measure.quantity.ElectricCharge;
import javax.measure.quantity.ElectricPotential;
import javax.measure.quantity.Power;

import nl.tno.dynaa.model.physical.Battery;
import nl.tno.dynaa.model.physical.CommunicationDevice;
import nl.tno.dynaa.model.physical.Memory;
import nl.tno.dynaa.model.physical.Node;
import nl.tno.dynaa.model.physical.Processor;
import tec.units.ri.quantity.Quantities;
import tec.units.ri.unit.Units;

/**
 * SampleAndCompressNode
 *
 * This class contains part of a physical view in the DynAA model.
 *
 * The DynAA "model" layer provides a basic template of a generic computational node. This template lays down a base for
 * describing processors and peripherals -- such of memory, communication devices, battery -- of a physical computation
 * device.
 *
 * A user (designer) may fill in this template with information about:
 *
 * -- The used processor, its several modes of operation, power used in each mode, etc.
 *
 * -- The used power source, e.g. battery and its power provision model
 *
 * -- The used memory module, its capacity, cache model, etc...
 *
 * -- Other standard or custom peripherals, such as communication devices, dedicated hardware, interruption controllers,
 * etc... Each peripheral has a minimal model to describe its modes of operations, and for each mode its performance and
 * power consumed.
 *
 * In this file, we custom model a standard node to reflect a small and fictive microprocessor.
 *
 */
public class SampleAndCompressNode {

    public static Node build() throws Exception {

        /**
         * Though DynAA is not limited to one way of modeling the power consumption, it usually applies a power
         * consumption tracing technique based on "modes" of operation.
         *
         * This technique determines a set of possible modes of operation for the processor, which is related to its
         * power consumption.
         * Power modes can express for example, the average power consumption during initialization, idle, computation,
         * etc...
         *
         * During the execution, tasks can send the processor to different modes of operation and keep them there for
         * some time.
         */
        final Map<String, Quantity<Power>> processingModes = new HashMap<>();
        processingModes.put("IDLE", Quantities.getQuantity(1.5e-6, Units.WATT));
        processingModes.put("BUSY", Quantities.getQuantity(1.2e-3, Units.WATT));
        final double numberOfIntegerOperationsPerSecond = 4.0323e6;
        final double numberOfFloatingPointOperationsPerSecond = 16.129e6;
        final Processor processor = new Processor("PROCESSOR",
                numberOfIntegerOperationsPerSecond,
                numberOfFloatingPointOperationsPerSecond,
                processingModes,
                "IDLE");

        final Memory memory = new Memory("MEMORY");

        /**
         * It is also possible to model the power source of the node.
         *
         * Here we describe as being it a small set of two batteries (1.5V) combined for a source voltage of 3V.
         * Standard batteries are used summing up to a battery charge of 7200Coulomb (which is approximately 2000mAh).
         */
        final Quantity<ElectricPotential> batteryPotential = Quantities.getQuantity(3.0, Units.VOLT);
        final Quantity<ElectricCharge> batteryCharge = Quantities.getQuantity(7200.0, Units.COULOMB);  // 2 Ah
        final Battery battery = new Battery("BATTERY", batteryPotential, batteryCharge);

        /**
         * We can also model any type of peripheral present at a platform (node).
         *
         * Here, we present as example a communication device (for example a radio). Its model use the same techniques
         * for mode of operations used with the processor.
         */
        final Map<String, Quantity<Power>> communicationModes = new HashMap<>();
        communicationModes.put("IDLE", Quantities.getQuantity(0.6e-6, Units.WATT));
        communicationModes.put("TX", Quantities.getQuantity(102e-3, Units.WATT));
        communicationModes.put("RX", Quantities.getQuantity(49.5e-3, Units.WATT));
        final CommunicationDevice communicationDevice = new CommunicationDevice("COMM_DEVICE", communicationModes, "IDLE");

        /**
         * Finally we create a node and configure it with the created peripherals.
         */
        final Node sampleAndCompressNode = new Node("SampleAndCompressNode", processor, memory, battery);
        sampleAndCompressNode.addPeripheral(communicationDevice);

        return sampleAndCompressNode;

    }
}
