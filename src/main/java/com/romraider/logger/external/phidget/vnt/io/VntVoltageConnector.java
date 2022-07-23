/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2022 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.romraider.logger.external.phidget.vnt.io;

import static org.apache.log4j.Logger.getLogger;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.phidget22.VoltageInput;
import com.phidget22.PhidgetException;

public final class VntVoltageConnector {
    private static final Logger LOGGER = getLogger(VntVoltageConnector.class);

    public static Set<VoltageInput> openIkSerial(
            final VntVoltageRunner ikr) {

        final Set<VoltageInput> kits = new HashSet<VoltageInput>();
        try {
            VoltageInput voltage0 = new VoltageInput();
            voltage0.setIsHubPortDevice(true);
            voltage0.setHubPort(0);
            voltage0.open(5000);

            VntVoltageSensorChangeListener listener = new VntVoltageSensorChangeListener(ikr);

            voltage0.addVoltageChangeListener(listener);

            kits.add(voltage0);

            LOGGER.info("VNT0 added");
        } catch (PhidgetException e) {
            LOGGER.error("InterfaceKit open error: " + e);
        }

        return kits;
    }
}
