package com.grouptuity.style.units;

import android.graphics.Color;
import com.grouptuity.Grouptuity;
import com.grouptuity.StyleTemplate;

public class ColorUnitForeground
{
	public static void addTo(StyleTemplate style)
	{
		style.primaryColor = Grouptuity.foregroundColor;
		style.borderColor = Grouptuity.lightColor;
		style.textColor = Color.BLACK;
	}
}