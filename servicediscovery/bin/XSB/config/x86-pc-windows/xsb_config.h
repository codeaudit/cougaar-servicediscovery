/* /cygdrive/c/xsbsys/XSBENV/XSB/config/x86-pc-windows/xsb_config.h.  Generated automatically by configure.  */
/*  @configure_input@ 
**
**   This file contains definitions for Windows
**
**   Some variable may have to be changed manually.
*/

/* File:      def_config_wind.in
** Author(s): kifer
** Contact:   xsb-contact@cs.sunysb.edu
** 
** Copyright (C) The Research Foundation of SUNY, 1998
** 
** XSB is free software; you can redistribute it and/or modify it under the
** terms of the GNU Library General Public License as published by the Free
** Software Foundation; either version 2 of the License, or (at your option)
** any later version.
** 
** XSB is distributed in the hope that it will be useful, but WITHOUT ANY
** WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
** FOR A PARTICULAR PURPOSE.  See the GNU Library General Public License for
** more details.
** 
** You should have received a copy of the GNU Library General Public License
** along with XSB; if not, write to the Free Software Foundation,
** Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
**
** $Id: xsb_config.h,v 1.1 2003-01-20 19:32:14 mthome Exp $
** 
*/



/******* VARIABLES THAT MIGHT NEED TO BE SET MANUALLY ****************/


/* If this is defined, SOCKET_LIBRARY should be set to wsock32.lib */
/* in MS_VC_Mfile.mak					           */
#define HAVE_SOCKET 1

/* Define as __inline__ if that's what the C compiler calls it.  */
#define inline __inline


/******* END Variables to be set manually ****************/


/**** DO NOT EDIT BELOW THIS LINE!!! *********************/
/**** Use these configuration options:
	--enable-local-scheduling   to set LOCAL_EVAL
	--with-oracle               to set ORACLE
	--enable-oracle-debug       to set both ORACLE and ORACLE_DEBUG
	--with-odbc                 to set XSB_ODBC
****/


#define WIN_NT 1
#define CC "gcc"
#define RELEASE_DATE 2002-03-11
#define XSB_VERSION "2.5 (Okocim)"
#if (defined(XSB_DLL) || defined(XSB_DLL_C))
#define FOREIGN_WIN32
#endif

/* this is used by many to check if config.h was included in proper order */
#ifndef CONFIG_INCLUDED
#define CONFIG_INCLUDED
#endif

/* Use local eval strategy. Default is `batched' */
#define LOCAL_EVAL 1

/* Define, if XSB is built with support for ORACLE DB */
/* #undef ORACLE */
/* #undef ORACLE_DEBUG */

/* Define if XSB is built with support for ODBC */
#define XSB_ODBC 1

/* Define if XSB is built with support for INTERPROLOG */
#define XSB_INTERPROLOG 1

#if (defined(FOREIGN_AOUT) || defined(FOREIGN_ELF) || defined(FOREIGN_WIN32))
#define FOREIGN
#endif

#define bcopy(A,B,L) memmove(B,A,L)

#define SLASH '\\'

/*  Files needed for XSB to get the configuration information */
#define CONFIGURATION "x86-pc-windows"
#define FULL_CONFIG_NAME "x86-pc-windows"

#define GC 1

/* The number of bytes in a long.  */
#define SIZEOF_LONG 4

/* Define if you have the gethostbyname function.  */
#define HAVE_GETHOSTBYNAME 1


/* Define if you have the mkdir function.  */
#define HAVE_MKDIR 1

/* Define if you have the snprintf function.  */
#define HAVE_SNPRINTF 1

/* Define if you have readline library */
#define HAVE_READLINE 1

/* Define if you have the strdup function.  */
#define HAVE_STRDUP 1

/* Define if you have the <malloc.h> header file.  */
#define HAVE_MALLOC_H 1

/* Define if you have the <stdlib.h> header file.  */
#define HAVE_STDLIB_H 1

/* Define if you have the <string.h> header file.  */
#define HAVE_STRING_H 1

/* GC on SLG-WAM! ;) */
#define SLG_GC 1

