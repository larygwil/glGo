/*
 * ogl_helper.cpp
 *
 * $Id: ogl_helper.cpp,v 1.8 2003/10/02 14:32:32 peter Exp $
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

#ifdef _WIN32
#include <windows.h>
#else
#include <string.h>
#endif

#include <GL/gl.h>
#include <GL/glext.h>
#include "ogl_helper.h"
#include <math.h>

#define TWOPI_  6.283185307179586476925287
#define PID2_   1.570796326794896619231322

// Extensions
#ifndef NO_MULTITEXTURES

// Supported by compiler?
#if !defined(GL_ARB_multitexture) || !defined(GL_ARB_multisample)
#error No ARB multitexture extension available
#endif

#ifdef _WIN32
PFNGLACTIVETEXTUREARBPROC glActiveTextureARB;
PFNGLMULTITEXCOORD2FARBPROC glMultiTexCoord2fARB;
#endif  // !_WIN32

#endif  // NO_MULTITEXTURES


extern const GLfloat j2[4] = { 0.25, 0.75, 0.75, 0.25 };
extern const GLfloat j8[16] = { 0.5625, 0.4375, 0.0625, 0.9375, 0.3125, 0.6875, 0.6875, 0.8125, 0.8125, 0.1875, 0.9375, 0.5625, 0.4375, 0.0625, 0.1875, 0.3125 };
extern const GLfloat j16[32] = { 0.375, 0.4375, 0.625, 0.0625, 0.875, 0.1875, 0.125, 0.0625, 0.375, 0.6875, 0.875, 0.4375, 0.625, 0.5625, 0.375, 0.9375, 0.625, 0.3125, 0.125, 0.5625, 0.125, 0.8125, 0.375, 0.1875, 0.875, 0.9375, 0.875, 0.6875, 0.125, 0.3125, 0.625, 0.8125 };

enum
{
  X, Y, Z, W
};

enum
{
  A, B, C, D
};

/** Simple struct defining a point in the 3D universe. */
struct XYZ
{
    GLfloat x, y, z;
};

void accFrustum(GLdouble left, GLdouble right, GLdouble bottom, GLdouble top,
                GLdouble near_v, GLdouble far_v, GLdouble pixdx, GLdouble pixdy,
                GLdouble eyedx, GLdouble eyedy, GLdouble focus)
{
    GLdouble xwsize, ywsize;
    GLdouble dx, dy;
    GLint viewport[4];
    glGetIntegerv (GL_VIEWPORT, viewport);
    xwsize = right - left;
    ywsize = top - bottom;
    dx = -(pixdx*xwsize/(GLdouble)viewport[2] + eyedx*near_v/focus);
    dy = -(pixdy*ywsize/(GLdouble)viewport[3] + eyedy*near_v/focus);
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    glFrustum(left + dx, right + dx, bottom + dy, top + dy, near_v, far_v);
    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();
    glTranslatef (-eyedx, -eyedy, 0.0);
}

void accPerspective(GLdouble fovy, GLdouble aspect,
                    GLdouble near_v, GLdouble far_v, GLdouble pixdx, GLdouble pixdy,
                    GLdouble eyedx, GLdouble eyedy, GLdouble focus)
{
    GLdouble fov2,left,right,bottom,top;
    fov2 = ((fovy*PI_) / 180.0) / 2.0;
    top = near_v / (cos(fov2) / sin(fov2));
    bottom = -top;
    right = top * aspect;
    left = -right;
    accFrustum (left, right, bottom, top, near_v, far_v, pixdx, pixdy, eyedx, eyedy, focus);
}

void shadowMatrix(GLfloat shadowMat[4][4], const GLfloat groundplane[4], const GLfloat lightpos[4])
{
  GLfloat dot;

  /* Find dot product between light position vector and ground plane normal. */
  dot = groundplane[X] * lightpos[X] +
    groundplane[Y] * lightpos[Y] +
    groundplane[Z] * lightpos[Z] +
    groundplane[W] * lightpos[W];

  shadowMat[0][0] = dot - lightpos[X] * groundplane[X];
  shadowMat[1][0] = 0.f - lightpos[X] * groundplane[Y];
  shadowMat[2][0] = 0.f - lightpos[X] * groundplane[Z];
  shadowMat[3][0] = 0.f - lightpos[X] * groundplane[W];

  shadowMat[X][1] = 0.f - lightpos[Y] * groundplane[X];
  shadowMat[1][1] = dot - lightpos[Y] * groundplane[Y];
  shadowMat[2][1] = 0.f - lightpos[Y] * groundplane[Z];
  shadowMat[3][1] = 0.f - lightpos[Y] * groundplane[W];

  shadowMat[X][2] = 0.f - lightpos[Z] * groundplane[X];
  shadowMat[1][2] = 0.f - lightpos[Z] * groundplane[Y];
  shadowMat[2][2] = dot - lightpos[Z] * groundplane[Z];
  shadowMat[3][2] = 0.f - lightpos[Z] * groundplane[W];

  shadowMat[X][3] = 0.f - lightpos[W] * groundplane[X];
  shadowMat[1][3] = 0.f - lightpos[W] * groundplane[Y];
  shadowMat[2][3] = 0.f - lightpos[W] * groundplane[Z];
  shadowMat[3][3] = dot - lightpos[W] * groundplane[W];
}

void findPlane(GLfloat plane[4], GLfloat v0[3], GLfloat v1[3], GLfloat v2[3])
{
  GLfloat vec0[3], vec1[3];

  /* Need 2 vectors to find cross product. */
  vec0[X] = v1[X] - v0[X];
  vec0[Y] = v1[Y] - v0[Y];
  vec0[Z] = v1[Z] - v0[Z];

  vec1[X] = v2[X] - v0[X];
  vec1[Y] = v2[Y] - v0[Y];
  vec1[Z] = v2[Z] - v0[Z];

  /* find cross product to get A, B, and C of plane equation */
  plane[A] = vec0[Y] * vec1[Z] - vec0[Z] * vec1[Y];
  plane[B] = -(vec0[X] * vec1[Z] - vec0[Z] * vec1[X]);
  plane[C] = vec0[X] * vec1[Y] - vec0[Y] * vec1[X];

  plane[D] = -(plane[A] * v0[X] + plane[B] * v0[Y] + plane[C] * v0[Z]);
}

bool supportsExtension(const char* extension)
{
    const GLubyte *all_extensions = glGetString(GL_EXTENSIONS);
    if (all_extensions == NULL)
        return false;
    return (strstr((const char *)all_extensions, extension) != NULL);
}

#ifdef _WIN32
bool initWin32Extensions()
{
    glActiveTextureARB = (PFNGLACTIVETEXTUREARBPROC)wglGetProcAddress("glActiveTextureARB");
    glMultiTexCoord2fARB = (PFNGLMULTITEXCOORD2FARBPROC)wglGetProcAddress("glMultiTexCoord2fARB");

    if (glActiveTextureARB == NULL || glMultiTexCoord2fARB == NULL)
        return false;
    return true;
}
#endif

void drawCircle(GLfloat radius)
{
    int i = 0;
    GLfloat _x = 0.0f;
    GLfloat _y = radius;
    i = 12;
    glBegin(GL_LINES);
    glVertex3f(_x, _y, 0.0f);
    while (i<=360)
    {
        _x = radius*sin(0.0174f * i);
        _y = radius*cos(0.0174f * i);
        glVertex3f(_x, _y, 0.0f);
        glVertex3f(_x, _y, 0.0f);
        i += 12;
    }
    glVertex3f(0.0f, radius, 0.0f);
    glEnd();
}

void drawRect(GLfloat width)
{
    glBegin(GL_LINES);

    glVertex3f(-width, -width, 0.0f);
    glVertex3f( width, -width, 0.0f);

    glVertex3f( width, -width, 0.0f);
    glVertex3f( width,  width, 0.0f);

    glVertex3f( width,  width, 0.0f);
    glVertex3f(-width,  width, 0.0f);

    glVertex3f(-width,  width, 0.0f);
    glVertex3f(-width, -width, 0.0f);

    glEnd();
}

void drawTriangle(GLfloat base)
{
    // Move it 20% upwards
    GLfloat offset = base * 0.2f;

    glBegin(GL_LINES);

    glVertex3f(-base, -base + offset, 0.0f);
    glVertex3f( base, -base + offset, 0.0f);

    glVertex3f( base, -base + offset, 0.0f);
    glVertex3f( 0.0f,  base + offset, 0.0f);

    glVertex3f( 0.0f,  base + offset, 0.0f);
    glVertex3f(-base, -base + offset, 0.0f);

    glEnd();
}

void drawCross(GLfloat width)
{
    glBegin(GL_LINES);

    glVertex3f(-width, -width, 0.0f);
    glVertex3f( width,  width, 0.0f);

    glVertex3f(-width,  width, 0.0f);
    glVertex3f( width, -width, 0.0f);

    glEnd();
}

void drawSphere(GLfloat r, GLuint n, bool multitex)
{
    GLfloat i, j;
    GLfloat theta1, theta2, theta3;
    XYZ c, e, p;
    c.x = c.y = c.z = 0;

    if (r < 0)
        r = -r;
    if (n < 4 || r <= 0)
    {
        glBegin(GL_POINTS);
        glVertex3f(c.x, c.y, c.z);
        glEnd();
        return;
    }

    for (j=0; j<static_cast<GLfloat>(n)/2; j++)
    {
        theta1 = j * TWOPI_ / static_cast<GLfloat>(n) - PID2_;
        theta2 = (j + 1.0f) * TWOPI_ / static_cast<GLfloat>(n) - PID2_;

        glBegin(GL_TRIANGLE_STRIP);
        for (i=0; i<=static_cast<GLfloat>(n); i++)
        {
            theta3 = i * TWOPI_ / static_cast<GLfloat>(n);

            e.x = cos(theta2) * cos(theta3);
            e.y = sin(theta2);
            e.z = cos(theta2) * sin(theta3);
            p.x = c.x + r * e.x;
            p.y = c.y + r * e.y;
            p.z = c.z + r * e.z;

            glNormal3f(e.x, e.y, e.z);
            glTexCoord2f(i/static_cast<GLfloat>(n), 2.0f*(j+1.0f)/static_cast<GLfloat>(n));
#ifndef NO_MULTITEXTURES
            if (multitex)
                glMultiTexCoord2fARB(GL_TEXTURE1_ARB,
                                     i/static_cast<GLfloat>(n), 2.0f*(j+1.0f)/static_cast<GLfloat>(n));
#endif
            glVertex3f(p.x, p.y, p.z);

            e.x = cos(theta1) * cos(theta3);
            e.y = sin(theta1);
            e.z = cos(theta1) * sin(theta3);
            p.x = c.x + r * e.x;
            p.y = c.y + r * e.y;
            p.z = c.z + r * e.z;

            glNormal3f(e.x, e.y, e.z);
            glTexCoord2f(i/static_cast<GLfloat>(n), 2.0f*j/static_cast<GLfloat>(n));
#ifndef NO_MULTITEXTURES
            if (multitex)
                glMultiTexCoord2fARB(GL_TEXTURE1_ARB,
                                     i/static_cast<GLfloat>(n), 2.0f*j/static_cast<GLfloat>(n));
#endif
            glVertex3f(p.x, p.y, p.z);
        }
        glEnd();
    }
}
