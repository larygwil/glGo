/*
 * ugf.h
 *
 * $Id: ugf.h,v 1.2 2003/11/24 14:38:58 peter Exp $
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

#ifndef UGF_H
#define UGF_H


/** @addtogroup ugf
 * @{ */

/**
 * @file
 *
 * This file contains the C code to call the embedded Python interpreter and
 * provides an API for callbacks from Python. The loaded Python module is
 * glGo.ugf_parser.py. In the distributed version, the Python files are located
 * in the share/pythonlib.zip archive.
 */

#ifdef __cplusplus
extern "C" {
#endif

    /**
     * Initialize Python and load the ugf_parser module.
     * This starts the embedded Python interpreter if it is not yet running and loads the glGo.ugf_parser module.
     * @param pylib Path to the module files, will be added to sys.path to let the interpreter find it
     * @return 0 on success, -1 on failure
     */
    int UGF_Init(const char *pylib);

    /**
     * Parse given UGF file.
     * The Python code will use the callbacks to tell the board about the read data.
     * @param filename File to load
     * @return 0 on success, -1 on failure
     */
    int UGF_Parse(const char *filename);

    /**
     * Set the C API callbacks.
     * These callbacks are used from the Python code to send the read data to the board, and are mapped
     * to functions in the BoardHandler class.
     * @param fp1 doMove callback
     * @param fp2 doComment callback
     * @param fp3 doMark callback
     * @param fp4 doHeader callback
     */
    void ugf_set_callbacks(void (*fp1)(int, int, int, int, int),    /* doMove */
                           void (*fp2)(const char*),                /* doComment */
                           void (*fp3)(int, int, const char*),      /* doMark */
                           void (*fp4)(const char*, const char*));  /* doHeader */

#ifdef __cplusplus
}
#endif

/** @} */  // End of group

#endif
