package com.savvytech.savvylevelfour.common;

//-----------------------------------------------------------------------------
//
//	Common
//
//	Author:		Mike Smits
//	Date:		14 Feb 17
//	Revision:	1.0.190214.1855
//
//-----------------------------------------------------------------------------

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Common {

    public static String readRawTextFile(Context context, int id) {
        InputStream inputStream = context.getResources().openRawResource(id);

        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader buf = new BufferedReader(in);

        String line;

        StringBuilder text = new StringBuilder();
        try {

            while (( line = buf.readLine()) != null) text.append(line);
        } catch (IOException e) {
            return null;

        }
        return text.toString();
    }


}
