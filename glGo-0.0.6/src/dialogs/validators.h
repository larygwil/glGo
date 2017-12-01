/*
 * validators.h
 *
 * $Id: validators.h,v 1.2 2003/10/02 14:16:51 peter Exp $
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

#ifndef VALIDATORS_H
#define VALIDATORS_H

#ifdef __GNUG__
#pragma interface "validators.h"
#endif


/**
 * @defgroup validators Validators
 * @ingroup userinterface
 *
 * These validators are used for the newgame and gameinfo dialogs
 * verifying the user input for the game data fields. The default
 * generic and text validators are not sufficient for board size,
 * handicap and komi validation.
 *
 * @{
 */


//------------------------------------------------------------------------
//                       Class BoardSizeValidator
//------------------------------------------------------------------------

/**
 * Validator for Boardsize. Can be applied to wxSpinCtrl.
 */
class BoardSizeValidator : public wxValidator
{
public:
    BoardSizeValidator(unsigned short *sz) : board_size(sz) {}
    BoardSizeValidator(const BoardSizeValidator &v) : board_size(v.board_size) {}
    virtual wxObject* Clone() const { return new BoardSizeValidator(*this); }
    virtual bool TransferToWindow();
    virtual bool TransferFromWindow();
    virtual bool Validate(wxWindow* parent);
    void OnChar(wxKeyEvent& event);

private:
    unsigned short *board_size;

DECLARE_EVENT_TABLE()
};


//------------------------------------------------------------------------
//                       Class HandicapValidator
//------------------------------------------------------------------------

/**
 * Validator for Handicap. Can be applied to wxTextCtrl and wxSpinCtrl.
 */
class HandicapValidator : public wxValidator
{
public:
    HandicapValidator(unsigned short *h) : handicap(h) {}
    HandicapValidator(const HandicapValidator &v) : handicap(v.handicap) {}
    virtual wxObject* Clone() const { return new HandicapValidator(*this); }
    virtual bool TransferToWindow();
    virtual bool TransferFromWindow();
    virtual bool Validate(wxWindow* parent);
    void OnChar(wxKeyEvent& event);

private:
    unsigned short *handicap;

DECLARE_EVENT_TABLE()
};


//------------------------------------------------------------------------
//                       Class KomiValidator
//------------------------------------------------------------------------

/**
 * Validator for Komi. Can be applied to wxTextCtrl and wxSpinCtrl.
 */
class KomiValidator : public wxValidator
{
public:
    KomiValidator(float *k) : komi(k) {}
    KomiValidator(const KomiValidator &v) : komi(v.komi) {}
    virtual wxObject* Clone() const { return new KomiValidator(*this); }
    virtual bool TransferToWindow();
    virtual bool TransferFromWindow();
    virtual bool Validate(wxWindow* parent) { return true; }
    void OnChar(wxKeyEvent& event);

private:
    float *komi;

DECLARE_EVENT_TABLE()
};

/** @} */  // End of group

#endif
