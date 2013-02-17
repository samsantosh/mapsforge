/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.map.layer;

import java.util.ArrayList;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.graphics.android.AndroidGraphics;
import org.mapsforge.map.graphics.Bitmap;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.queue.Job;
import org.mapsforge.map.layer.queue.JobQueue;
import org.mapsforge.map.model.MapViewPosition;

import android.graphics.Canvas;
import android.graphics.Matrix;

public abstract class TileLayer<T extends Job> extends Layer {
	private final TileCache<T> tileCache;
	protected final JobQueue<T> jobQueue;

	public TileLayer(TileCache<T> tileCache, MapViewPosition mapViewPosition) {
		if (tileCache == null) {
			throw new IllegalArgumentException("tileCache must not be null");
		} else if (mapViewPosition == null) {
			throw new IllegalArgumentException("mapViewPosition must not be null");
		}

		this.tileCache = tileCache;
		this.jobQueue = new JobQueue<T>(mapViewPosition);
	}

	@Override
	public void destroy() {
		this.tileCache.destroy();
	}

	@Override
	public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point canvasPosition) {
		ArrayList<TilePosition> tilePositions = LayerUtil.getTilePositions(boundingBox, zoomLevel, canvasPosition);

		for (int i = tilePositions.size() - 1; i >= 0; --i) {
			TilePosition tilePosition = tilePositions.get(i);
			Point point = tilePosition.point;
			Tile tile = tilePosition.tile;
			Bitmap bitmap = this.tileCache.get(createJob(tile));

			if (bitmap == null) {
				this.jobQueue.add(createJob(tile));
				drawParentTileBitmap(canvas, point, tile);
			} else {
				android.graphics.Bitmap androidBitmap = AndroidGraphics.getAndroidBitmap(bitmap);
				canvas.drawBitmap(androidBitmap, (float) point.x, (float) point.y, null);
			}
		}

		this.jobQueue.notifyWorkers();
	}

	private void drawParentTileBitmap(Canvas canvas, Point point, Tile tile) {
		Tile cachedParentTile = getCachedParentTile(tile, 4);
		if (cachedParentTile != null) {
			Bitmap bitmap = this.tileCache.get(createJob(cachedParentTile));
			if (bitmap != null) {
				long translateX = tile.getShiftX(cachedParentTile) * Tile.TILE_SIZE;
				long translateY = tile.getShiftY(cachedParentTile) * Tile.TILE_SIZE;
				byte zoomLevelDiff = (byte) (tile.zoomLevel - cachedParentTile.zoomLevel);
				float scaleFactor = (float) Math.pow(2, zoomLevelDiff);

				Matrix matrix = new Matrix();
				matrix.setScale(scaleFactor, scaleFactor);
				matrix.postTranslate((float) (point.x - translateX), (float) (point.y - translateY));
				android.graphics.Bitmap androidBitmap = AndroidGraphics.getAndroidBitmap(bitmap);
				canvas.drawBitmap(androidBitmap, matrix, null);
			}
		}
	}

	/**
	 * @return the first parent object of the given object whose tileCacheBitmap is cached (may be null).
	 */
	private Tile getCachedParentTile(Tile tile, int level) {
		if (level == 0) {
			return null;
		}

		Tile parentTile = tile.getParent();
		if (parentTile == null) {
			return null;
		} else if (this.tileCache.containsKey(createJob(parentTile))) {
			return parentTile;
		}

		return getCachedParentTile(parentTile, level - 1);
	}

	protected abstract T createJob(Tile tile);
}