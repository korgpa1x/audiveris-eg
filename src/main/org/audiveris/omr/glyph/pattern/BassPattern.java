//----------------------------------------------------------------------------//
//                                                                            //
//                           B a s s P a t t e r n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
// Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//----------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.glyph.pattern;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import org.audiveris.omr.glyph.CompoundBuilder;
import org.audiveris.omr.glyph.CompoundBuilder.CompoundAdapter;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.glyph.facets.Glyph;

import org.audiveris.omr.grid.StaffInfo;

import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code BassPattern} checks for segmented bass clefs, in the
 * neighborhood of typical vertical two-dot patterns
 *
 * @author Hervé Bitteur
 */
public class BassPattern
        extends GlyphPattern
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            BassPattern.class);

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // BassPattern //
    //-------------//
    /**
     * Creates a new BassPattern object.
     *
     * @param system the containing system
     */
    public BassPattern (SystemInfo system)
    {
        super("Bass", system);
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // runPattern //
    //------------//
    @Override
    public int runPattern ()
    {
        int successNb = 0;

        // Constants for clef verification
        final double maxBassDotPitchDy = constants.maxBassDotPitchDy.getValue();
        final double maxBassDotDx = scale.toPixels(constants.maxBassDotDx);

        // Specific adapter definition for bass clefs
        CompoundAdapter bassAdapter = new BassAdapter(
                system,
                Grades.clefMinGrade);

        for (Glyph top : system.getGlyphs()) {
            // Look for top dot
            if ((top.getShape() != Shape.DOT_set)
                || (Math.abs(top.getPitchPosition() - -3) > maxBassDotPitchDy)) {
                continue;
            }

            int topX = top.getCentroid().x;
            StaffInfo topStaff = system.getStaffAt(top.getCentroid());

            // Look for bottom dot right underneath, and in the same staff
            for (Glyph bot : system.getGlyphs()) {
                if ((bot.getShape() != Shape.DOT_set)
                    || (Math.abs(bot.getPitchPosition() - -1) > maxBassDotPitchDy)) {
                    continue;
                }

                if (Math.abs(bot.getCentroid().x - topX) > maxBassDotDx) {
                    continue;
                }

                if (system.getStaffAt(bot.getCentroid()) != topStaff) {
                    continue;
                }

                // Here we have a couple
                logger.debug(
                        "Got bass dots #{} & #{}",
                        top.getId(),
                        bot.getId());

                Glyph compound = system.buildCompound(
                        top,
                        true,
                        system.getGlyphs(),
                        bassAdapter);

                if (compound != null) {
                    successNb++;
                }
            }
        }

        return successNb;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxBassDotDx = new Scale.Fraction(
                0.25,
                "Tolerance on Bass dot abscissae");

        Constant.Double maxBassDotPitchDy = new Constant.Double(
                "pitch",
                0.5,
                "Ordinate tolerance on a Bass dot pitch position");

    }

    //-------------//
    // BassAdapter //
    //-------------//
    /**
     * This is the compound adapter meant to build bass clefs
     */
    private class BassAdapter
            extends CompoundBuilder.TopShapeAdapter
    {
        //~ Constructors -------------------------------------------------------

        public BassAdapter (SystemInfo system,
                            double minGrade)
        {
            super(system, minGrade, ShapeSet.BassClefs);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Rectangle computeReferenceBox ()
        {
            if (seed == null) {
                throw new NullPointerException(
                        "Compound seed has not been set");
            }

            Rectangle pixRect = new Rectangle(seed.getCentroid());
            pixRect.add(
                    new Point(
                    pixRect.x - (2 * scale.getInterline()),
                    pixRect.y + (3 * scale.getInterline())));

            return pixRect;
        }

        @Override
        public boolean isCandidateSuitable (Glyph glyph)
        {
            return !glyph.isManualShape()
                   || ShapeSet.BassClefs.contains(glyph.getShape());
        }
    }
}
