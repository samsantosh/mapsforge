/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.mapsforge.map.layer.renderer;

import java.util.List;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.SymbolContainer;
import org.mapsforge.core.model.Point;

final class WayDecorator {

	/**
	 * Minimum distance in pixels before the way name is repeated.
	 */
	private static final int DISTANCE_BETWEEN_WAY_NAMES = 500;

	static void renderSymbol(Bitmap symbolBitmap, float dy, boolean alignCenter,
	                         boolean repeatSymbol, float repeatGap, float repeatStart,
	                         boolean rotate, Point[][] coordinates,
			List<SymbolContainer> waySymbols) {
		int skipPixels = (int)repeatStart;

		Point[] c;
		if (dy == 0f) {
			c = coordinates[0];
		} else {
			c = RendererUtils.parallelPath(coordinates[0], dy);
		}

		// get the first way point coordinates
		double previousX = c[0].x;
		double previousY = c[0].y;

		// draw the symbol on each way segment
		float segmentLengthRemaining;
		float segmentSkipPercentage;
		float theta = 0f;
		for (int i = 1; i < coordinates[0].length; ++i) {
			// get the current way point coordinates
			double currentX = c[i].x;
			double currentY = c[i].y;

			// calculate the length of the current segment (Euclidian distance)
			double diffX = currentX - previousX;
			double diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);
			segmentLengthRemaining = (float) segmentLengthInPixel;

			while (segmentLengthRemaining - skipPixels > repeatStart) {
				// calculate the percentage of the current segment to skip
				segmentSkipPercentage = skipPixels / segmentLengthRemaining;

				// move the previous point forward towards the current point
				previousX += diffX * segmentSkipPercentage;
				previousY += diffY * segmentSkipPercentage;
				if (rotate) {
					// if we do not rotate theta will be 0, which is correct
					theta = (float) Math.atan2(currentY - previousY, currentX - previousX);
				}
				Point point = new Point(previousX, previousY);
				waySymbols.add(new SymbolContainer(symbolBitmap, point, alignCenter, theta));

				// check if the symbol should only be rendered once
				if (!repeatSymbol) {
					return;
				}

				// recalculate the distances
				diffX = currentX - previousX;
				diffY = currentY - previousY;

				// recalculate the remaining length of the current segment
				segmentLengthRemaining -= skipPixels;

				// set the amount of pixels to skip before repeating the symbol
				skipPixels = (int)repeatGap;
			}

			skipPixels -= segmentLengthRemaining;
			if (skipPixels < repeatStart) {
				skipPixels = (int)repeatStart;
			}

			// set the previous way point coordinates for the next loop
			previousX = currentX;
			previousY = currentY;
		}
	}

	static void renderText(String textKey, float dy, Paint fill, Paint stroke, Point[][] coordinates,
			List<WayTextContainer> wayNames) {
		// calculate the way name length plus some margin of safety
		int wayNameWidth = fill.getTextWidth(textKey) + 10;

		int skipPixels = 0;

		Point[] c;
		if (dy == 0f) {
			c = coordinates[0];
		} else {
			c = RendererUtils.parallelPath(coordinates[0], dy);
		}

		// get the first way point coordinates
		double previousX = c[0].x;
		double previousY = c[0].y;

		// find way segments long enough to draw the way name on them
		for (int i = 1; i < c.length; ++i) {
			// get the current way point coordinates
			double currentX = c[i].x;
			double currentY = c[i].y;

			// calculate the length of the current segment (Euclidian distance)
			double diffX = currentX - previousX;
			double diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);

			if (skipPixels > 0) {
				skipPixels -= segmentLengthInPixel;
			} else if (segmentLengthInPixel > wayNameWidth) {
				int x1;
				int x2;
				int y1;
				int y2;

				// check to prevent inverted way names
				if (previousX <= currentX) {
					x1 = (int) previousX;
					y1 = (int) previousY;
					x2 = (int) currentX;
					y2 = (int) currentY;
				} else {
					x1 = (int) currentX;
					y1 = (int) currentY;
					x2 = (int) previousX;
					y2 = (int) previousY;
				}

				wayNames.add(new WayTextContainer(x1, y1, x2, y2, textKey, fill));
				if (stroke != null) {
					wayNames.add(new WayTextContainer(x1, y1, x2, y2, textKey, stroke));
				}

				skipPixels = DISTANCE_BETWEEN_WAY_NAMES;
			}

			// store the previous way point coordinates
			previousX = currentX;
			previousY = currentY;
		}
	}

	private WayDecorator() {
		throw new IllegalStateException();
	}
}
