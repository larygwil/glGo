/*
 * playerdb.c
 *
 * $Id: playerdb.c,v 1.9 2003/11/18 17:09:56 peter Exp $
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

#ifndef WIN32
#include <python2.3/Python.h>
#else
#include <Python.h>
#endif
#include "playerdb.h"

#define BUF_SIZE 32
/* #define PY_DEBUG */

static PyObject *pPlayerDBModule = NULL;

int PlayerDB_Init(const char *pylib)
{
    char pypath[128];

    if (pPlayerDBModule != NULL)
    {
        printf("Module already loaded.\n");
        return 0;
    }

#ifdef PY_DEBUG
    /* Load directly from python files instead from the archive.
     * Adjust to your personal path if you need this. */
    /* pylib = "/home/peter/development/glGo/python/playerdb"; */
    pylib = "C:\\work\\playerdb";
#endif

    sprintf(pypath, "sys.path.insert(0, \"%s\")\n", pylib);
    printf("import module: %s\n", pylib);

    /* Load the interpreter */
    Py_Initialize();

    /* Tell the python interpreter where to find the glGo module.
     * Setting PYTHONPATH with putenv works on Linux but not on
     * Windoze (why not?), but the following trick here works on
     * both systems. */
    PyRun_SimpleString("import sys\n");
    PyRun_SimpleString(pypath);

    /* Try importing the glGo.playerdb module */
    PyErr_Clear();
    pPlayerDBModule = PyImport_ImportModule("glGo.playerdb");
    if (pPlayerDBModule == NULL)
    {
        printf("Failed to import playerdb module\n");
        if (PyErr_Occurred() != NULL)
            PyErr_Print();
        PlayerDB_Quit();
        return -1;
    }
    printf("Playerdb module loaded.\n");
    return 0;
}

void PlayerDB_Quit()
{
    printf("Quitting python...\n");
    if (pPlayerDBModule != NULL)
    {
#if !(defined(__VISUALC__) && defined(_DEBUG))
        /* MSVC coughs on this in debug mode. This will create a
         * memory leak, but I don't care as it's in debug mode only. */
        Py_DECREF(pPlayerDBModule);
#endif
        Py_Finalize();
        pPlayerDBModule = NULL;
    }
    else
        printf("Nothing to quit...\n");
}

int PlayerDB_LoadDB(const char *filename)
{
    PyObject *pValue;
    int res;

    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    pValue = PyObject_CallMethod(pPlayerDBModule, "load_db", "(s)", filename, 0);
    if (pValue == NULL)
        return -1;
    res = PyInt_AsLong(pValue);
#if !(defined(__VISUALC__) && defined(_DEBUG))
    Py_DECREF(pValue);
#endif
    return res;
}

int PlayerDB_SaveDB(const char *filename)
{
    PyObject *pValue;
    int res;

    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    pValue = PyObject_CallMethod(pPlayerDBModule, "save_db", "(s)", filename, 0);
    if (pValue == NULL)
        return -1;
    res = PyInt_AsLong(pValue);
#if !(defined(__VISUALC__) && defined(_DEBUG))
    Py_DECREF(pValue);
#endif
    return res;
}

int PlayerDB_CheckReloadDB()
{
    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    PyObject_CallMethod(pPlayerDBModule, "check_db_reload", 0);
    return 0;
}

int PlayerDB_AddPlayer(const char *name, int status)
{
    PyObject *pValue;
    int res;

    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -2;
    }

    pValue = PyObject_CallMethod(pPlayerDBModule, "add_player", "(si)", name, status, 0);
    if (pValue == NULL)
        return -2;
    res = PyInt_AsLong(pValue);
#if !(defined(__VISUALC__) && defined(_DEBUG))
    Py_DECREF(pValue);
#endif
    return res;
}

int PlayerDB_RemovePlayer(const char *name)
{
    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    PyObject_CallMethod(pPlayerDBModule, "remove_player", "(s)", name, 0);
    return 0;
}

int PlayerDB_GetPlayerStatus(const char *name)
{
    PyObject *pValue;
    int res;

    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    pValue = PyObject_CallMethod(pPlayerDBModule, "get_player_status", "(s)", name, 0);
    if (pValue == NULL)
        return -1;
    res = PyInt_AsLong(pValue);
#if !(defined(__VISUALC__) && defined(_DEBUG))
    Py_DECREF(pValue);
#endif
    return res;
}

char* PlayerDB_GetPlayerComment(const char *name)
{
    PyObject *pValue;
    char *s, *comment;

    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return NULL;
    }

    pValue = PyObject_CallMethod(pPlayerDBModule, "get_player_comment", "(s)", name, 0);
    if (pValue == NULL)
        return NULL;
    s = PyString_AsString(pValue);
    comment = (char*)malloc(sizeof(char) * strlen(s));
    strcpy(comment, s);
#if !(defined(__VISUALC__) && defined(_DEBUG))
    Py_DECREF(pValue);
#endif
    return comment;
}

int PlayerDB_SetPlayerComment(const char *name, const char *comment)
{
    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    PyObject_CallMethod(pPlayerDBModule, "set_player_comment", "(ss)", name, comment, 0);
    return 0;
}

int PlayerDB_GetPlayerFlag(const char *name, int flag)
{
    PyObject *pValue;
    int res;

    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    pValue = PyObject_CallMethod(pPlayerDBModule, "get_player_flag", "(si)", name, flag, 0);
    if (pValue == NULL)
        return -1;
    res = PyInt_AsLong(pValue);
#if !(defined(__VISUALC__) && defined(_DEBUG))
    Py_DECREF(pValue);
#endif
    return res;
}

int PlayerDB_SetPlayerFlag(const char *name, int flag, int value)
{
    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    PyObject_CallMethod(pPlayerDBModule, "set_player_flag", "(sii)", name, flag, value, 0);
    return 0;
}

char** PlayerDB_GetPlayerList(int status, int *size)
{
    PyObject *pValue, *pObj;
    int i;
    char **list;

    if (pPlayerDBModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        *size = -1;
        return NULL;
    }

    pValue = PyObject_CallMethod(pPlayerDBModule, "list_players", "(i)", status, 0);
    if (pValue == NULL)
    {
        printf("No return value, aborting.\n");
        *size = -1;
        return NULL;
    }

    *size = PyList_Size(pValue);
    list = (char**)malloc(sizeof(char) * BUF_SIZE * *size);
    for (i=0; i<*size; i++)
    {
        if ((pObj = PyList_GetItem(pValue, i)) != NULL)
        {
            char *s = PyString_AsString(pObj);
            if (s != NULL)
            {
                list[i] = (char*)malloc(sizeof(char) * BUF_SIZE);
                strcpy(list[i], s);
            }
        }
    }

#if !(defined(__VISUALC__) && defined(_DEBUG))
    Py_DECREF(pValue);
#endif
    return list;
}


#ifdef STANDALONE

/*
 * Debug stuff
 */
int main()
{
    /* int i, size;
       char **list; */
    int i;

    if (PlayerDB_Init("pythonlib.zip") < 0)
    {
        printf("Failed to load module.\n");
        return 0;
    }

    if (PlayerDB_LoadDB(NULL) < 0)
        printf("Failed to load database.\n");

#if 0
    list = PlayerDB_GetPlayerList(1, &size);
    printf("Size: %d\n", size);
    for (i=0; i<size; i++)
        printf("%s\n", list[i]);
#endif

    PlayerDB_SetPlayerFlag("tweet", 0, 1);
    PlayerDB_SetPlayerComment("tweet", "Hoho\nfoobar");
    i = PlayerDB_GetPlayerFlag("tweet", 0);
    printf("Flag: %d\n", i);
    PlayerDB_SaveDB(NULL);

    PlayerDB_Quit();
    return 0;
}

#endif  /** STANDALONE */
