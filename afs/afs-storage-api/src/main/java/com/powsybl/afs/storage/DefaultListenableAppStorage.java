/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage;

import com.powsybl.afs.storage.events.*;
import com.powsybl.commons.util.WeakListenerList;
import com.powsybl.math.timeseries.DoubleArrayChunk;
import com.powsybl.math.timeseries.StringArrayChunk;
import com.powsybl.math.timeseries.TimeSeriesMetadata;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class DefaultListenableAppStorage extends ForwardingAppStorage implements ListenableAppStorage {

    private final WeakListenerList<AppStorageListener> listeners = new WeakListenerList<>();

    private NodeEventList eventList = new NodeEventList();

    private final Lock lock = new ReentrantLock();

    public DefaultListenableAppStorage(AppStorage storage) {
        super(storage);
    }

    private void addEvent(NodeEvent event) {
        lock.lock();
        try {
            eventList.addEvent(event);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public NodeInfo createRootNodeIfNotExists(String name, String nodePseudoClass) {
        return super.createRootNodeIfNotExists(name, nodePseudoClass);
    }

    @Override
    public NodeInfo createNode(String parentNodeId, String name, String nodePseudoClass, String description, int version, NodeGenericMetadata genericMetadata) {
        NodeInfo nodeInfo = super.createNode(parentNodeId, name, nodePseudoClass, description, version, genericMetadata);
        addEvent(new ChildAdded(nodeInfo.getId(), parentNodeId));
        return nodeInfo;
    }

    @Override
    public void setDescription(String nodeId, String description) {
        super.setDescription(nodeId, description);
        addEvent(new NodeDescriptionUpdated(nodeId, description));
    }

    @Override
    public void setParentNode(String nodeId, String newParentNodeId) {
        String oldParentNodeId = getParentNode(nodeId).map(NodeInfo::getId).orElseThrow(AssertionError::new);
        super.setParentNode(nodeId, newParentNodeId);
        addEvent(new ChildRemoved(nodeId, oldParentNodeId));
        addEvent(new ChildAdded(nodeId, newParentNodeId));
    }

    @Override
    public String deleteNode(String nodeId) {
        String parentNodeId = super.deleteNode(nodeId);
        addEvent(new ChildRemoved(nodeId, parentNodeId));
        return parentNodeId;
    }

    @Override
    public OutputStream writeBinaryData(String nodeId, String name) {
        OutputStream os = super.writeBinaryData(nodeId, name);
        addEvent(new NodeDataUpdated(nodeId, name));
        return os;
    }

    @Override
    public boolean removeData(String nodeId, String name) {
        boolean removed = super.removeData(nodeId, name);
        if (removed) {
            addEvent(new NodeDataRemoved(nodeId, name));
        }
        return removed;
    }

    @Override
    public void createTimeSeries(String nodeId, TimeSeriesMetadata metadata) {
        super.createTimeSeries(nodeId, metadata);
        addEvent(new TimeSeriesCreated(nodeId, metadata.getName()));
    }

    @Override
    public void addDoubleTimeSeriesData(String nodeId, int version, String timeSeriesName, List<DoubleArrayChunk> chunks) {
        super.addDoubleTimeSeriesData(nodeId, version, timeSeriesName, chunks);
        addEvent(new TimeSeriesDataAdded(nodeId, timeSeriesName));
    }

    @Override
    public void addStringTimeSeriesData(String nodeId, int version, String timeSeriesName, List<StringArrayChunk> chunks) {
        super.addStringTimeSeriesData(nodeId, version, timeSeriesName, chunks);
        addEvent(new TimeSeriesDataAdded(nodeId, timeSeriesName));
    }

    @Override
    public void clearTimeSeries(String nodeId) {
        super.clearTimeSeries(nodeId);
        addEvent(new TimeSeriesCleared(nodeId));
    }

    @Override
    public void addDependency(String nodeId, String name, String toNodeId) {
        super.addDependency(nodeId, name, toNodeId);
        addEvent(new DependencyAdded(nodeId, name));
        addEvent(new BackwardDependencyAdded(toNodeId, name));
    }

    @Override
    public void removeDependency(String nodeId, String name, String toNodeId) {
        super.removeDependency(nodeId, name, toNodeId);
        addEvent(new DependencyRemoved(nodeId, name));
        addEvent(new BackwardDependencyRemoved(toNodeId, name));
    }

    @Override
    public void flush() {
        super.flush();
        lock.lock();
        try {
            listeners.log();
            listeners.notify((l, info) -> {
                NodeEventFilter filter = (NodeEventFilter) info;
                l.onEvents(new NodeEventList(eventList.getEvents().stream()
                                                                  .filter(e -> e.getId().equals(filter.getNodeId())
                                                                          && filter.getNodeClass().isAssignableFrom(e.getClass()))
                                                                  .collect(Collectors.toList())));
            });
            eventList = new NodeEventList();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addListener(AppStorageListener l, NodeEventFilter filter) {

        listeners.add(l, filter);
    }

    @Override
    public void removeListener(AppStorageListener l) {
        listeners.remove(l);
    }

    @Override
    public void removeListeners() {
        listeners.removeAll();
    }
}
