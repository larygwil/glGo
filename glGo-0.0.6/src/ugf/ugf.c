/*
 * ugf.c
 *
 * $Id: ugf.c,v 1.2 2003/11/24 01:59:39 peter Exp $
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

#include "ugf.h"

/* #define UGF_PY_DEBUG */

static PyObject *pUGFModule = NULL;


/*
 * Callbacks for BoardHandler
 */

static void (*doMove_cb)(int, int, int, int, int);
static void (*doComment_cb)(const char*);
static void (*doMark_cb)(int, int, const char*);
static void (*doHeader_cb)(const char*, const char*);

void ugf_set_callbacks(void (*fp1)(int, int, int, int, int),
                       void (*fp2)(const char*),
                       void (*fp3)(int, int, const char*),
                       void (*fp4)(const char*, const char*))
{
    doMove_cb = fp1;
    doComment_cb = fp2;
    doMark_cb = fp3;
    doHeader_cb = fp4;
}


/*
 * Python extensions callbacks
 */

static PyObject* ugf_do_move(PyObject *self, PyObject *args)
{
    int x, y, col, num, time;

    if (!PyArg_ParseTuple(args, "((ii)iii)", &x, &y, &col, &num, &time))
        return NULL;

    // printf("ugf_do_move: %d/%d %d %d %d\n", x, y, col, num, time);

    if (doMove_cb != NULL)
        (*doMove_cb)(x, y, col, num, time);

    Py_INCREF(Py_None);
    return Py_None;
}

static PyObject* ugf_do_comment(PyObject *self, PyObject *args)
{
    char *comment;

    if (!PyArg_ParseTuple(args, "s", &comment))
        return NULL;

    /* printf("ugf_do_comment: %s\n", comment); */

    if (doComment_cb != NULL)
        (*doComment_cb)(comment);

    Py_INCREF(Py_None);
    return Py_None;
}

static PyObject* ugf_do_mark(PyObject *self, PyObject *args)
{
    int x, y;
    char *text;

    if (!PyArg_ParseTuple(args, "(ii)s", &x, &y, &text))
        return NULL;

    /* printf("ugf_do_mark: %d/%d %s\n", x, y, text); */

    if (doMark_cb != NULL)
        (*doMark_cb)(x, y, text);

    Py_INCREF(Py_None);
    return Py_None;
}

static PyObject* ugf_do_header(PyObject *self, PyObject *args)
{
    char *key, *value;

    if (!PyArg_ParseTuple(args, "ss", &key, &value))
        return NULL;

    /* printf("ugf_do_header: %s - %s\n", key, value); */

    if (doHeader_cb != NULL)
        (*doHeader_cb)(key, value);

    Py_INCREF(Py_None);
    return Py_None;
}

static PyMethodDef ugfMethods[] =
{
    { "ugf_do_move", ugf_do_move, METH_VARARGS, "Play a move."},
    { "ugf_do_comment", ugf_do_comment, METH_VARARGS, "Add a comment to a move."},
    { "ugf_do_mark", ugf_do_mark, METH_VARARGS, "Add a mark to a move."},
    { "ugf_do_header", ugf_do_header, METH_VARARGS, "Set a header key/value pair."},
    {NULL, NULL, 0, NULL}        /* Sentinel */
};

PyMODINIT_FUNC initugf(void)
{
    Py_InitModule("ugf", ugfMethods);
}


/*
 * Embedded Python
 */

int UGF_Init(const char *pylib)
{
    char pypath[128];

    if (pUGFModule != NULL)
    {
        printf("Module already loaded.\n");
        return 0;
    }

#ifdef UGF_PY_DEBUG
    /* Load directly from python files instead from the archive.
     * Adjust to your personal path if you need this. */
    pylib = "/home/peter/development/glGo/python/playerdb";
#endif

    sprintf(pypath, "sys.path.insert(0, \"%s\")\n", pylib);
    printf("import module: %s\n", pylib);

    /* Load the interpreter */
    Py_Initialize();

    /* Init callback module */
    initugf();

    /* Tell the python interpreter where to find the glGo module.
     * Setting PYTHONPATH with putenv works on Linux but not on
     * Windoze (why not?), but the following trick here works on
     * both systems. */
    PyRun_SimpleString("import sys\n");
    PyRun_SimpleString(pypath);

    /* Try importing the glGo.ugf_parser module */
    PyErr_Clear();
    pUGFModule = PyImport_ImportModule("glGo.ugf_parser");
    if (pUGFModule == NULL)
    {
        printf("Failed to import ugf module\n");
        if (PyErr_Occurred() != NULL)
            PyErr_Print();
        return -1;
    }
    printf("Ugf module loaded.\n");

    return 0;
}

int UGF_Parse(const char *filename)
{
    PyObject *pValue;
    int res;

    if (pUGFModule == NULL)
    {
        printf("Module not loaded, aborting.\n");
        return -1;
    }

    pValue = PyObject_CallMethod(pUGFModule, "parseUGF", "(s)", filename, 0);
    if (pValue == NULL)
        return -1;
    res = PyInt_AsLong(pValue);
#if !(defined(__VISUALC__) && defined(_DEBUG))
    Py_DECREF(pValue);
#endif
    return res;
}


/*
 * Debug stuff
 */
#ifdef STANDALONE

void UGF_Quit(void)
{
    printf("Quitting UGF module...\n");
#if !(defined(__VISUALC__) && defined(_DEBUG))
    if (pUGFModule != NULL)
        Py_DECREF(pUGFModule);
#endif
}

int main(int argc, char *argv[])
{
    if (UGF_Init("pythonlib.zip") < 0)
    {
        printf("Failed to load module.\n");
        return -1;
    }

    int res = UGF_Parse(argv[1]);
    printf("Res: %d\n", res);

    UGF_Quit();
    Py_Finalize()

    return 0;
}
#endif  /** STANDALONE */
