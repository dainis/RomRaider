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

package com.romraider.logger.external.phidget.vnt.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.Action;

import com.romraider.logger.ecu.EcuLogger;
import com.romraider.logger.external.core.ExternalDataItem;
import com.romraider.logger.external.core.ExternalDataSource;
import com.romraider.logger.external.phidget.vnt.io.VntVoltageManager;
import com.romraider.logger.external.phidget.vnt.io.VntVoltageRunner;
import com.romraider.logger.external.phidget.vnt.io.VntVoltageSensor;
import com.romraider.util.ThreadUtil;

/**
 * The IntfKitDataSource class is called when the Logger starts up and the
 * call to load the external plug-ins is made. This class with its helpers
 * will open each PhidgetInterfaceKit and find all available inputs. It will
 * interrogate the inputs then dynamically build a list of inputs found based
 * on the serial number and input number.
 * 
 * @see ExternalDataSource
 */
public final class VntVoltageDataSource implements ExternalDataSource {
    private final Map<String, VntVoltageDataItem> dataItems = new HashMap<String, VntVoltageDataItem>();
    private VntVoltageRunner runner;
    private List<Integer> kits;

    {

        VntVoltageManager.loadIk();

        final Set<VntVoltageSensor> sensors = VntVoltageManager.getSensors();
        for (Iterator<VntVoltageSensor> sensorIt = sensors.iterator(); sensorIt.hasNext();) {
            final VntVoltageSensor sensor = sensorIt.next();
            dataItems.put(sensor.getInputName(), new VntVoltageDataItem(sensor));
        }

    }

    public String getId() {
        return getClass().getName();
    }

    public String getName() {
        return "Phidget VNT";
    }

    public String getVersion() {
        return "0.01";
    }

    public List<? extends ExternalDataItem> getDataItems() {
        return Collections.unmodifiableList(
                new ArrayList<VntVoltageDataItem>(dataItems.values()));
    }

    public Action getMenuAction(final EcuLogger logger) {
        return new VntVoltagePluginMenuAction(logger);
    }

    public void setProperties(Properties properties) {
    }

    public void connect() {
        runner = new VntVoltageRunner(kits, dataItems);
        ThreadUtil.runAsDaemon(runner);
    }

    public void disconnect() {
        if (runner != null)
            runner.stop();
    }

    public void setPort(final String port) {
    }

    public String getPort() {
        return "HID USB";
    }
}
