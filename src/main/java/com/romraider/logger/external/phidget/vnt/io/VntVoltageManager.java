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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.phidget22.Phidget;
import com.phidget22.PhidgetException;
import com.romraider.util.SettingsManager;

public final class VntVoltageManager {
    private static final Logger LOGGER = getLogger(VntVoltageManager.class);

    public static void loadIk() {
        try {
            LOGGER.info("Plugin found: " + Phidget.getLibraryVersion());
        } catch (PhidgetException e) {
            LOGGER.error("InterfaceKit error: " + e);
        }
    }

    public static Set<VntVoltageSensor> getSensors() {
        Set<VntVoltageSensor> sensors = new HashSet<VntVoltageSensor>();

        Map<String, VntVoltageSensor> phidgets = SettingsManager.getSettings().getPhidgetVntVoltageSensors();
        if (phidgets == null) {
            phidgets = new HashMap<String, VntVoltageSensor>();
        }

        final String key = "VNT0";
        if (phidgets.containsKey(key)) {
            sensors.add(phidgets.get(key));
            final String stored = String.format(
                    "Plugin applying user settings for: %s",
                    phidgets.get(key).toString());
            LOGGER.info(stored);
        } else {
            final VntVoltageSensor sensor = new VntVoltageSensor();
            
            sensor.setInputNumber(0);
            sensor.setInputName(key);
            sensor.setUnits("raw value");
            sensor.setExpression("x");
            sensor.setFormat("0.00");
            sensor.setMinValue(0);
            sensor.setMaxValue(5);
            sensor.setStepValue((float) 0.01);
            sensors.add(sensor);
            phidgets.put(key, sensor);
        }

        SettingsManager.getSettings().setPhidgetVntVoltageSensors(phidgets);

        return sensors;
    }
}
