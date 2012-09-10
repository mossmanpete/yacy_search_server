// TablesColumnBLOBIndex.java
// (C) 2012 by Stefan Foerster, sof@gmx.de, Norderstedt, Germany
// first published 2012 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// LICENSE
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.yacy.kelondro.blob;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import net.yacy.cora.util.SpaceExceededException;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.util.ByteBuffer;
import de.anomic.data.ymark.YMarkUtil;

public class TablesColumnBLOBIndex extends TablesColumnIndex{

	// Map<ColumnName, Map<ColumnValue, T<PrimaryKey>>>
	private final BEncodedHeap index;	
	private final static byte SEPERATOR = (byte) ',';
	
    public TablesColumnBLOBIndex(final BEncodedHeap bheap) {    	
    	super(TablesColumnIndex.INDEXTYPE.BLOB);
    	this.index = bheap;    	
    }
    
    public static Collection<byte[]> byteToCollection(final byte[] b) {
    	final Collection<byte[]> PKset = ByteBuffer.split(b, SEPERATOR);
    	return PKset;
    }
    
    public static byte[] CollectionToByte(final Collection<byte[]> bc) {
    	final ByteBuffer buf = new ByteBuffer(15 * bc.size());
    	final Iterator<byte[]> iter = bc.iterator();
    	while(iter.hasNext()) {
    		buf.append(iter.next());
    		buf.append(SEPERATOR);
    	}
    	return buf.getBytes();
    }
    
    public void deleteIndex(final String columnName) {
    	final byte[] column = YMarkUtil.getKeyId(columnName);
    	try {
			this.index.remove(column);
		} catch (IOException e) {
			Log.logException(e);
		} catch (SpaceExceededException e) {
			Log.logException(e);
		}
    }
         
	protected void  insertPK(final String columnName, final String columnValue, final byte[] pk) {
		Map<String, byte[]> valueIdxMap;
		Collection<byte[]> PKset;	
		final byte[] column = YMarkUtil.getKeyId(columnName);
		try {
			valueIdxMap = this.index.get(column);
			if(valueIdxMap != null) {				
				if(valueIdxMap.containsKey(columnValue)) {
					PKset = byteToCollection(valueIdxMap.get(columnValue));	
					if(!ByteBuffer.contains(PKset, pk)) {
						PKset.add(pk);
					}
				} else {
					PKset = new ArrayList<byte[]>(1);
					PKset.add(pk);
					valueIdxMap.put(columnValue, CollectionToByte(PKset));
				}
			} else {
				PKset = new ArrayList<byte[]>(1);
				PKset.add(pk);
				valueIdxMap = new ConcurrentHashMap<String, byte[]>();
			}
			valueIdxMap.put(columnValue, CollectionToByte(PKset));	
			this.index.insert(column, valueIdxMap);
			return;
		} catch (IOException e) {
			Log.logException(e);
		} catch (SpaceExceededException e) {
			Log.logException(e);
		}	
	}
	
	protected void removePK(final byte[] pk) {
		final Iterator<Map.Entry<byte[], Map<String, byte[]>>> niter = this.index.iterator();		
		while (niter.hasNext()) {
			final Map.Entry<byte[], Map<String,byte[]>> entry = niter.next();
			final Iterator<Map.Entry<String, byte[]>> viter = entry.getValue().entrySet().iterator();
			while(viter.hasNext()) {
				final Map.Entry<String, byte[]> columnValue = viter.next();
				final Collection<byte[]> PKset = byteToCollection(columnValue.getValue());
				ByteBuffer.remove(PKset, pk);			
				if(PKset.isEmpty()) {
					viter.remove();					
				} else {
					columnValue.setValue(CollectionToByte(PKset));	
				}
			}
			try {
				this.index.insert(entry.getKey(), entry.getValue());
			} catch (SpaceExceededException e) {
				Log.logException(e);
			} catch (IOException e) {
				Log.logException(e);
			}
		}
	}
	
	public void clear() {
		this.index.clear();
	}
	
	public Collection<String> columns() {
		return this.index.columns();
	}
	
	public Set<String> keySet(final String columnName) {
		final byte[] column = YMarkUtil.getKeyId(columnName);
		// a TreeSet is used to get sorted set of keys (e.g. folders)
		if(this.index.containsKey(column)) {
			try {
				return new TreeSet<String>(this.index.get(column).keySet());
			} catch (IOException e) {
				Log.logException(e);
			} catch (SpaceExceededException e) {
				Log.logException(e);
			}
		}		
		return new TreeSet<String>();
	}
	
	public boolean containsKey(final String columnName, final String key) {
		final byte[] column = YMarkUtil.getKeyId(columnName);
		if(this.index.containsKey(column)) {
			try {
				return this.index.get(column).containsKey(key);
			} catch (IOException e) {
				Log.logException(e);
			} catch (SpaceExceededException e) {
				Log.logException(e);
			}
		}
		return false;
	}
	
	public boolean hasIndex(final String columnName) {
		final byte[] column = YMarkUtil.getKeyId(columnName);
		return this.index.containsKey(column);
	}
	
	public Collection<byte[]> get(final String columnName, final String key) {
		final byte[] column = YMarkUtil.getKeyId(columnName);
		// deserialize
		try {
			return byteToCollection(this.index.get(column).get(key));
		} catch (IOException e) {
			Log.logException(e);
		} catch (SpaceExceededException e) {
			Log.logException(e);
		}
		return new ArrayList<byte[]>();
	}
	
	public int size(final String columnName) {
		final byte[] column = YMarkUtil.getKeyId(columnName);
		if(this.index.containsKey(column)) {
			try {
				return this.index.get(column).size();
			} catch (IOException e) {
				Log.logException(e);
			} catch (SpaceExceededException e) {
				Log.logException(e);
			}
		}
		return -1;
	}	
	
	public int size() {
		return this.index.size();
	}
}