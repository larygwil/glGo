/*
 * ogl_helper.h
 *
 * $Id: ogl_helper.h,v 1.8 2003/10/02 14:32:33 peter Exp $
 *
 * glGo, a prototype for a 3D Goban based on wxWindows, OpenGL and SDL.
 * Copyright (c) 2003, Peter Strempel <pstrempel@gmx.de>
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
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/** @addtogroup utils
 * @{ */

/**
 * @file
 * This file contains common global functions used by the OpenGL board.
 */

#ifndef OGLHELPER_H
#define OGLHELPER_H

#define PI_ 3.141592653589793238462643

extern const GLfloat j2[4], j8[16], j16[32];  ///< Jitter table

/**
 * Global OpenGL helper function used for the scene antialias jitter calculation.
 * This is a simple reimplementation of glFrustum() which accepts jitter parameters.
 */
void accFrustum(
    GLdouble left, GLdouble right, GLdouble bottom, GLdouble top,
    GLdouble near_v, GLdouble far_v, GLdouble pixdx, GLdouble pixdy,
    GLdouble eyedx, GLdouble eyedy, GLdouble focus);

/**
 * Global OpenGL helper function used for the scene antialias jitter calculation.
 * This is a simple reimplementation of gluPerspective() which accepts jitter parameters.
 */
void accPerspective(
    GLdouble fovy, GLdouble aspect, GLdouble near_v, GLdouble far_v,
    GLdouble pixdx, GLdouble pixdy,
    GLdouble eyedx, GLdouble eyedy, GLdouble focus);

/** Create a matrix that will project the desired shadow. */
void shadowMatrix(GLfloat shadowMat[4][4], const GLfloat groundplane[4], const GLfloat lightpos[4]);

/** Find the plane equation given 3 points. */
void findPlane(GLfloat plane[4], GLfloat v0[3], GLfloat v1[3], GLfloat v2[3]);

/**
 * Check if an extension given by its string definition is supported by
 * this renderer.
 * @param extension Extension to check for, for example "GL_ARB_multitexture".
 * @return True if supported, false if not supported.
 */
bool supportsExtension(const char* extension);

#ifdef _WIN32
/** Initialize Win32 extension pointers. */
bool initWin32Extensions();
#endif

/** Draw a circle at with given radius. */
void drawCircle(GLfloat radius);

/** Draw an unfilled rectangle with given width. */
void drawRect(GLfloat width);

/** Draw a triangle with the given base size. */
void drawTriangle(GLfloat base);

/** Draw a cross with the given width. */
void drawCross(GLfloat width);

/**
 * Draw a sphere with radius r and n fragments.
 * The sphere is created from triangle strips which should be quite fast.
 * 32 seems to be a reasonable value for n.
 * @param r Radius
 * @param n Number of triangle fragments
 * @param multitex If true, create multitexture coordinages. Make sure
 *                 the extension is supported before calling this!
 */
void drawSphere(GLfloat r, GLuint n, bool multitex);

/* @} */

#endif
