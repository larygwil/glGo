/*
 * playerdb.h
 *
 * $Id: playerdb.h,v 1.5 2003/11/17 12:46:45 peter Exp $
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

/**
 * @file
 * This code calls the PlayerDatabase python module using the embedded
 * Python interpreter.
 */

#ifndef PLAYERDB_H
#define PLAYERDB_H

#ifdef __cplusplus
extern "C" {
#endif

    int PlayerDB_Init(const char *pylib);
    void PlayerDB_Quit();
    int PlayerDB_LoadDB(const char *filename);
    int PlayerDB_SaveDB(const char *filename);
    int PlayerDB_CheckReloadDB();
    int PlayerDB_AddPlayer(const char *name, int status);
    int PlayerDB_RemovePlayer(const char *name);
    int PlayerDB_GetPlayerStatus(const char *name);
    char* PlayerDB_GetPlayerComment(const char *name);
    int PlayerDB_SetPlayerComment(const char *name, const char *comment);
    int PlayerDB_GetPlayerFlag(const char *name, int flag);
    int PlayerDB_SetPlayerFlag(const char *name, int flag, int value);
    char** PlayerDB_GetPlayerList(int status, int *size);

#ifdef __cplusplus
}
#endif

#endif
