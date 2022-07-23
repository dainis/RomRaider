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

import static com.romraider.util.ThreadUtil.sleep;
import static org.apache.log4j.Logger.getLogger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.phidget22.PhidgetException;
import com.phidget22.VoltageInput;
import com.romraider.logger.external.core.Stoppable;
import com.romraider.logger.external.phidget.vnt.plugin.VntVoltageDataItem;

public final class VntVoltageRunner implements Stoppable {
    private static final Logger LOGGER = getLogger(VntVoltageRunner.class);
    private final Map<String, VntVoltageDataItem> dataItems;
    private Set<VoltageInput> connections;
    private boolean stop;

    public VntVoltageRunner(
            final List<Integer> kits,
            final Map<String, VntVoltageDataItem> dataItems) {
        this.dataItems = dataItems;
        this.connections = VntVoltageConnector.openIkSerial(this);
    }

    @Override
    public void run() {
        try {
            while (!stop) {
                sleep(500L);
            }
            for (VoltageInput connector : connections) {
                connector.close();
            }
        } catch (PhidgetException e) {
            LOGGER.error("InterfaceKit close error: " + e);
        }
    }

    @Override
    public void stop() {
        stop = true;
    }

    public void updateDataItem(final String inputName, final double value) {
        dataItems.get(inputName).setData(value);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.info(String.format("Phidget VINT sensor event - raw value: %d", value));
        }
    }
}
