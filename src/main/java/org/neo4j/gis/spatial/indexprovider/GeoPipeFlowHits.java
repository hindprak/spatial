/**
 * Copyright (c) 2010-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gis.spatial.indexprovider;

import org.neo4j.gis.spatial.EditableLayer;
import org.neo4j.gis.spatial.SpatialDatabaseRecord;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.pipes.GeoPipeFlow;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.CatchingIteratorWrapper;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.Iterator;
import java.util.List;

/**
 * I've replaced the {@link org.neo4j.gis.spatial.indexprovider.GeoPipeFlowHits} class with this one, which is
 * based on the {@link org.neo4j.index.impl.lucene.IdToEntityIterator} used by the Lucene index. If a node
 * is found to be missing, then the <tt>itemDodged()</tt> method simply removes
 * it from the layer
 *
 * @author ben
 *
 */
public class GeoPipeFlowHits extends CatchingIteratorWrapper<Node, GeoPipeFlow> implements IndexHits<Node> {
	private final int size;
	private SpatialDatabaseService spatialDatabase;
	private EditableLayer layer;
    private GeoPipeFlow current;

    public GeoPipeFlowHits(List<GeoPipeFlow> hits, EditableLayer layer) {
		super(hits.iterator());
		this.size = hits.size();
		this.spatialDatabase = layer.getSpatialDatabase();
		this.layer = layer;
	}

	public int size() {
		return this.size;
	}

	public float currentScore() {
        Number value = null;
        if (current!=null && current.hasProperty("OrthodromicDistance")) value =  (Number)current.getProperty("OrthodromicDistance");
		return value == null ? 0 : value.floatValue();
	}

	@Override
	public Iterator<Node> iterator() {
		return this;
	}

	@Override
	public void close() {
		//Not sure if this means anything when operating on a Collection
	}

	@Override
	public Node getSingle() {
		try {
			return IteratorUtil.singleOrNull((Iterator<Node>) this);
		} finally {
			close();
		}
	}

	@Override
	protected Node underlyingObjectToObject(GeoPipeFlow current) {
        this.current = current;

        SpatialDatabaseRecord record = current.getRecord();
        // It looks to be possible to have SpatialDatabaseRecords without any
		// associated 'real' node. If this is the case is it OK to just return
		// null or should we return the geomNode
		
		Object idString = record.getProperty("id");
		Node result = null;
		
		if(idString != null){
			result = spatialDatabase.getDatabase().getNodeById(Long.valueOf(idString.toString()));
		}
		
		return result;
	}

	@Override
	protected void itemDodged(GeoPipeFlow item) {
		layer.delete(item.getRecord().getNodeId());
	}

	@Override
	protected boolean exceptionOk(Throwable t) {
		return t instanceof NotFoundException;
	}

}
