package com.grepbugs.plugin.eclipse.ui;


/*Copyright (c) 2011 Google, Inc.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Google, Inc. - initial API and implementation
*******************************************************************************/


import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;


/**
* Utility class for managing OS resources associated with SWT controls such as colors, fonts, images, etc.
* <p>
* !!! IMPORTANT !!! Application code must explicitly invoke the <code>dispose()</code> method to release the
* operating system resources managed by cached objects when those objects and OS resources are no longer
* needed (e.g. on application shutdown)
* <p>
* This class may be freely distributed as part of any application or plugin.
* <p>
* @author scheglov_ke
* @author Dan Rubel
*/
public class SWTResourceManager {
       ////////////////////////////////////////////////////////////////////////////
       //
       // Color
       //
       ////////////////////////////////////////////////////////////////////////////
       private static Map<RGB, Color> m_colorMap = new HashMap<RGB, Color>();
       /**
        * Returns the system {@link Color} matching the specific ID.
        * 
        * @param systemColorID
        *            the ID value for the color
        * @return the system {@link Color} matching the specific ID
        */
       public static Color getColor(int systemColorID) {
               Display display = Display.getCurrent();
               return display.getSystemColor(systemColorID);
       }
       
       public static Color getDarkColor(Color clr){
         return getColor((int)(0.45*clr.getRed()), (int)(0.45*clr.getGreen()),(int) (0.45*clr.getBlue()));
       }
       /**
        * Returns a {@link Color} given its red, green and blue component values.
        * 
        * @param r
        *            the red component of the color
        * @param g
        *            the green component of the color
        * @param b
        *            the blue component of the color
        * @return the {@link Color} matching the given red, green and blue component values
        */
       public static Color getColor(int r, int g, int b) {
               return getColor(new RGB(r, g, b));
       }
       /**
        * Returns a {@link Color} given its RGB value.
        * 
        * @param rgb
        *            the {@link RGB} value of the color
        * @return the {@link Color} matching the RGB value
        */
       public static Color getColor(RGB rgb) {
               Color color = m_colorMap.get(rgb);
               if (color == null) {
                       Display display = Display.getCurrent();
                       color = new Color(display, rgb);
                       m_colorMap.put(rgb, color);
               }
               return color;
       }
       /**
        * Dispose of all the cached {@link Color}'s.
        */
       public static void disposeColors() {
               for (Color color : m_colorMap.values()) {
                       color.dispose();
               }
               m_colorMap.clear();
       }
       ////////////////////////////////////////////////////////////////////////////
       //
       // Image
       //
       ////////////////////////////////////////////////////////////////////////////
       /**
        * Maps image paths to images.
        */
       private static Map<String, Image> m_imageMap = new HashMap<String, Image>();
       /**
        * Returns an {@link Image} encoded by the specified {@link InputStream}.
        * 
        * @param stream
        *            the {@link InputStream} encoding the image data
        * @return the {@link Image} encoded by the specified input stream
        */
       protected static Image getImage(InputStream stream) throws IOException {
               try {
                       Display display = Display.getCurrent();
                       ImageData data = new ImageData(stream);
                       if (data.transparentPixel > 0) {
                               return new Image(display, data, data.getTransparencyMask());
                       }
                       return new Image(display, data);
               } finally {
                       stream.close();
               }
       }
       
       public static Image loadURLImage(String url){
         Image image = m_imageMap.get(url);
         if (image == null) {
         try {
           BufferedImage img = ImageIO.read(new URL(url));
           ImageData imgData= convertToSWT(img);
           int width = imgData.width;
           int height = imgData.height;
           int w = width, h = height;
           while(w / 2 > 50 && h/2 > 50){
             w = w/2;
             h = h/2;
           }
           imgData = imgData.scaledTo(w,h);
           image = new Image(Display.getDefault(), imgData);
         } catch (Exception e) {
           //image = getMissingImage();
           //Logger.log("Image not found for url "+ url);
         }
         m_imageMap.put(url, image);
         }
         return image;
       }
       
       public static Image loadURLImage(String url, int w, int h){
         Image image = m_imageMap.get(url);
         if (image == null) {
         try {
           BufferedImage img = ImageIO.read(new URL(url));
           ImageData imgData= convertToSWT(img);
           imgData = imgData.scaledTo(w,h);
           image = new Image(Display.getDefault(), imgData);
         } catch (Exception e) {
           //image = getMissingImage();
           //Logger.log("Image not found for url "+ url);
         }
         m_imageMap.put(url, image);
         }
         return image;
       }
       /**
        * converts bufferedimage to ImageData
        * @param bufferedImage
        * @return
        */
       public static ImageData convertToSWT(BufferedImage bufferedImage) {
         if (bufferedImage.getColorModel() instanceof DirectColorModel) {
             DirectColorModel colorModel
                     = (DirectColorModel) bufferedImage.getColorModel();
             PaletteData palette = new PaletteData(colorModel.getRedMask(),
                     colorModel.getGreenMask(), colorModel.getBlueMask());
             ImageData data = new ImageData(bufferedImage.getWidth(),
                     bufferedImage.getHeight(), colorModel.getPixelSize(),
                     palette);
             WritableRaster raster = bufferedImage.getRaster();
             int[] pixelArray = new int[3];
             for (int y = 0; y < data.height; y++) {
                 for (int x = 0; x < data.width; x++) {
                     raster.getPixel(x, y, pixelArray);
                     int pixel = palette.getPixel(new RGB(pixelArray[0],
                             pixelArray[1], pixelArray[2]));
                     data.setPixel(x, y, pixel);
                 }
             }
             return data;
         }
         else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
             IndexColorModel colorModel = (IndexColorModel)
                     bufferedImage.getColorModel();
             int size = colorModel.getMapSize();
             byte[] reds = new byte[size];
             byte[] greens = new byte[size];
             byte[] blues = new byte[size];
             colorModel.getReds(reds);
             colorModel.getGreens(greens);
             colorModel.getBlues(blues);
             RGB[] rgbs = new RGB[size];
             for (int i = 0; i < rgbs.length; i++) {
                 rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
                         blues[i] & 0xFF);
             }
             PaletteData palette = new PaletteData(rgbs);
             ImageData data = new ImageData(bufferedImage.getWidth(),
                     bufferedImage.getHeight(), colorModel.getPixelSize(),
                     palette);
             data.transparentPixel = colorModel.getTransparentPixel();
             WritableRaster raster = bufferedImage.getRaster();
             int[] pixelArray = new int[1];
             for (int y = 0; y < data.height; y++) {
                 for (int x = 0; x < data.width; x++) {
                     raster.getPixel(x, y, pixelArray);
                     data.setPixel(x, y, pixelArray[0]);
                 }
             }
             return data;
         }else if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
           ComponentColorModel colorModel = (ComponentColorModel)bufferedImage.getColorModel();

           //ASSUMES: 3 BYTE BGR IMAGE TYPE

           PaletteData palette = new PaletteData(0x0000FF, 0x00FF00,0xFF0000);
           ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);

           //This is valid because we are using a 3-byte Data model with no transparent pixels
           data.transparentPixel = -1;

           WritableRaster raster = bufferedImage.getRaster();
           int[] pixelArray = new int[3];
           for (int y = 0; y < data.height; y++) {
               for (int x = 0; x < data.width; x++) {
                   raster.getPixel(x, y, pixelArray);
                   int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
                   data.setPixel(x, y, pixel);
               }
           }
           return data;
         }
         return null;
     }
       
       /**
        * Returns an {@link Image} stored in the file at the specified path.
        * 
        * @param path
        *            the path to the image file
        * @return the {@link Image} stored in the file at the specified path
        */
       public static Image getImage(String path) {
               Image image = m_imageMap.get(path);
               if (image == null) {
                       try {
                               image = null; //Activator.getDefault().getImage(path);
                               if (image == null)
                                 image = getImage(new FileInputStream(path));
                               m_imageMap.put(path, image);
                       } catch (Exception e) {
                               image = getMissingImage();
                               m_imageMap.put(path, image);
                       }
               }
               return image;
       }
       
       /**
        * Adding new method to put an image into map
        * @param path
        * @param newImage
        */
       public static void putImage (String path, Image newImage) {
         if (m_imageMap.containsKey(path)) {           
           Image oldImage = m_imageMap.get(path);
           oldImage.dispose();           
         }
         
         m_imageMap.put(path, newImage);
       }
       
       public static boolean containsImage (String path) {
         return m_imageMap.containsKey(path);
       }
       
       /**
        * Returns an {@link Image} stored in the file at the specified path relative to the specified class.
        * 
        * @param clazz
        *            the {@link Class} relative to which to find the image
        * @param path
        *            the path to the image file, if starts with <code>'/'</code>
        * @return the {@link Image} stored in the file at the specified path
        */
       public static Image getImage(Class<?> clazz, String path) {
               String key = clazz.getName() + '|' + path;
               Image image = m_imageMap.get(key);
               if (image == null) {
                       try {
                               image = getImage(clazz.getResourceAsStream(path));
                               m_imageMap.put(key, image);
                       } catch (Exception e) {
                               image = getMissingImage();
                               m_imageMap.put(key, image);
                       }
               }
               return image;
       }
       private static final int MISSING_IMAGE_SIZE = 10;
       /**
        * @return the small {@link Image} that can be used as placeholder for missing image.
        */
       private static Image getMissingImage() {
               Image image = new Image(Display.getCurrent(), MISSING_IMAGE_SIZE, MISSING_IMAGE_SIZE);
               //
               GC gc = new GC(image);
               gc.setBackground(getColor(SWT.COLOR_RED));
               gc.fillRectangle(0, 0, MISSING_IMAGE_SIZE, MISSING_IMAGE_SIZE);
               gc.dispose();
               //
               return image;
       }
       /**
        * Style constant for placing decorator image in top left corner of base image.
        */
       public static final int TOP_LEFT = 1;
       /**
        * Style constant for placing decorator image in top right corner of base image.
        */
       public static final int TOP_RIGHT = 2;
       /**
        * Style constant for placing decorator image in bottom left corner of base image.
        */
       public static final int BOTTOM_LEFT = 3;
       /**
        * Style constant for placing decorator image in bottom right corner of base image.
        */
       public static final int BOTTOM_RIGHT = 4;
       /**
        * Internal value.
        */
       protected static final int LAST_CORNER_KEY = 5;
       /**
        * Maps images to decorated images.
        */
       @SuppressWarnings("unchecked")
       private static Map<Image, Map<Image, Image>>[] m_decoratedImageMap = new Map[LAST_CORNER_KEY];
       /**
        * Returns an {@link Image} composed of a base image decorated by another image.
        * 
        * @param baseImage
        *            the base {@link Image} that should be decorated
        * @param decorator
        *            the {@link Image} to decorate the base image
        * @return {@link Image} The resulting decorated image
        */
       public static Image decorateImage(Image baseImage, Image decorator) {
               return decorateImage(baseImage, decorator, BOTTOM_RIGHT);
       }
       /**
        * Returns an {@link Image} composed of a base image decorated by another image.
        * 
        * @param baseImage
        *            the base {@link Image} that should be decorated
        * @param decorator
        *            the {@link Image} to decorate the base image
        * @param corner
        *            the corner to place decorator image
        * @return the resulting decorated {@link Image}
        */
       public static Image decorateImage(final Image baseImage, final Image decorator, final int corner) {
               if (corner <= 0 || corner >= LAST_CORNER_KEY) {
                       throw new IllegalArgumentException("Wrong decorate corner");
               }
               Map<Image, Map<Image, Image>> cornerDecoratedImageMap = m_decoratedImageMap[corner];
               if (cornerDecoratedImageMap == null) {
                       cornerDecoratedImageMap = new HashMap<Image, Map<Image, Image>>();
                       m_decoratedImageMap[corner] = cornerDecoratedImageMap;
               }
               Map<Image, Image> decoratedMap = cornerDecoratedImageMap.get(baseImage);
               if (decoratedMap == null) {
                       decoratedMap = new HashMap<Image, Image>();
                       cornerDecoratedImageMap.put(baseImage, decoratedMap);
               }
               //
               Image result = decoratedMap.get(decorator);
               if (result == null) {
                       Rectangle bib = baseImage.getBounds();
                       Rectangle dib = decorator.getBounds();
                       //
                       result = new Image(Display.getCurrent(), bib.width, bib.height);
                       //
                       GC gc = new GC(result);
                       gc.drawImage(baseImage, 0, 0);
                       if (corner == TOP_LEFT) {
                               gc.drawImage(decorator, 0, 0);
                       } else if (corner == TOP_RIGHT) {
                               gc.drawImage(decorator, bib.width - dib.width, 0);
                       } else if (corner == BOTTOM_LEFT) {
                               gc.drawImage(decorator, 0, bib.height - dib.height);
                       } else if (corner == BOTTOM_RIGHT) {
                               gc.drawImage(decorator, bib.width - dib.width, bib.height - dib.height);
                       }
                       gc.dispose();
                       //
                       decoratedMap.put(decorator, result);
               }
               return result;
       }
       /**
        * Dispose all of the cached {@link Image}'s.
        */
       public static void disposeImages() {
               // dispose loaded images
               {
                       for (Image image : m_imageMap.values()) {
                               image.dispose();
                       }
                       m_imageMap.clear();
               }
               // dispose decorated images
               for (int i = 0; i < m_decoratedImageMap.length; i++) {
                       Map<Image, Map<Image, Image>> cornerDecoratedImageMap = m_decoratedImageMap[i];
                       if (cornerDecoratedImageMap != null) {
                               for (Map<Image, Image> decoratedMap : cornerDecoratedImageMap.values()) {
                                       for (Image image : decoratedMap.values()) {
                                               image.dispose();
                                       }
                                       decoratedMap.clear();
                               }
                               cornerDecoratedImageMap.clear();
                       }
               }
       }
       ////////////////////////////////////////////////////////////////////////////
       //
       // Font
       //
       ////////////////////////////////////////////////////////////////////////////
       /**
        * Maps font names to fonts.
        */
       private static Map<String, Font> m_fontMap = new HashMap<String, Font>();
       /**
        * Maps fonts to their bold versions.
        */
       private static Map<Font, Font> m_fontToBoldFontMap = new HashMap<Font, Font>();
       
       /**
        * Maps fonts to their italic versions.
        */
       private static Map<Font, Font> m_fontToItalicFontMap = new HashMap<Font, Font>();
       /**
        * Returns a {@link Font} based on its name, height and style.
        * 
        * @param name
        *            the name of the font
        * @param height
        *            the height of the font
        * @param style
        *            the style of the font
        * @return {@link Font} The font matching the name, height and style
        */
       public static Font getFont(String name, int height, int style) {
               return getFont(name, height, style, false, false);
       }
       /**
        * Returns a {@link Font} based on its name, height and style. Windows-specific strikeout and underline
        * flags are also supported.
        * 
        * @param name
        *            the name of the font
        * @param size
        *            the size of the font
        * @param style
        *            the style of the font
        * @param strikeout
        *            the strikeout flag (warning: Windows only)
        * @param underline
        *            the underline flag (warning: Windows only)
        * @return {@link Font} The font matching the name, height, style, strikeout and underline
        */
       public static Font getFont(String name, int size, int style, boolean strikeout, boolean underline) {
               String fontName = name + '|' + size + '|' + style + '|' + strikeout + '|' + underline;
               Font font = m_fontMap.get(fontName);
               if (font == null) {
                       FontData fontData = new FontData(name, size, style);
                       if (strikeout || underline) {
                               try {
                                       Class<?> logFontClass = Class.forName("org.eclipse.swt.internal.win32.LOGFONT"); //$NON-NLS-1$
                                       Object logFont = FontData.class.getField("data").get(fontData); //$NON-NLS-1$
                                       if (logFont != null && logFontClass != null) {
                                               if (strikeout) {
                                                       logFontClass.getField("lfStrikeOut").set(logFont, Byte.valueOf((byte) 1)); //$NON-NLS-1$
                                               }
                                               if (underline) {
                                                       logFontClass.getField("lfUnderline").set(logFont, Byte.valueOf((byte) 1)); //$NON-NLS-1$
                                               }
                                       }
                               } catch (Throwable e) {
                                       System.err.println("Unable to set underline or strikeout" + " (probably on a non-Windows platform). " + e); //$NON-NLS-1$ //$NON-NLS-2$
                               }
                       }
                       font = new Font(Display.getCurrent(), fontData);
                       m_fontMap.put(fontName, font);
               }
               return font;
       }
       /**
        * Returns a bold version of the given {@link Font}.
        * 
        * @param baseFont
        *            the {@link Font} for which a bold version is desired
        * @return the bold version of the given {@link Font}
        */
       public static Font getBoldFont(Font baseFont) {
               Font font = m_fontToBoldFontMap.get(baseFont);
               if (font == null) {
                       FontData fontDatas[] = baseFont.getFontData();
                       FontData data = fontDatas[0];
                       font = new Font(Display.getCurrent(), data.getName(), data.getHeight(), SWT.BOLD);
                       m_fontToBoldFontMap.put(baseFont, font);
               }
               return font;
       }
       
       /**
        * Returns a italic version of the given {@link Font}.
        * 
        * @param baseFont
        *            the {@link Font} for which a bold version is desired
        * @return the bold version of the given {@link Font}
        */
       public static Font getItalicFont(Font baseFont) {
               Font font = m_fontToItalicFontMap.get(baseFont);
               if (font == null) {
                       FontData fontDatas[] = baseFont.getFontData();
                       FontData data = fontDatas[0];
                       font = new Font(Display.getCurrent(), data.getName(), data.getHeight(), SWT.ITALIC);
                       m_fontToItalicFontMap.put(baseFont, font);
               }
               return font;
       }
       
       /**
        * Dispose all of the cached {@link Font}'s.
        */
       public static void disposeFonts() {
               // clear fonts
               for (Font font : m_fontMap.values()) {
                       font.dispose();
               }
               m_fontMap.clear();
               // clear bold fonts
               for (Font font : m_fontToBoldFontMap.values()) {
                       font.dispose();
               }
               m_fontToBoldFontMap.clear();
               
               // clear bold fonts
               for (Font font : m_fontToItalicFontMap.values()) {
                       font.dispose();
               }
               m_fontToItalicFontMap.clear();
       }
       ////////////////////////////////////////////////////////////////////////////
       //
       // Cursor
       //
       ////////////////////////////////////////////////////////////////////////////
       /**
        * Maps IDs to cursors.
        */
       private static Map<Integer, Cursor> m_idToCursorMap = new HashMap<Integer, Cursor>();
       
       
       /**
        * Returns the system cursor matching the specific ID.
        * 
        * @param id
        *            int The ID value for the cursor
        * @return Cursor The system cursor matching the specific ID
        */
       public static Cursor getCursor(int id) {
               Integer key = Integer.valueOf(id);
               Cursor cursor = m_idToCursorMap.get(key);
               if (cursor == null) {
                       cursor = new Cursor(Display.getDefault(), id);
                       m_idToCursorMap.put(key, cursor);
               }
               return cursor;
       }
       
       
       private static Map<Image, Cursor> m_imageToCursorMap = new HashMap<Image, Cursor>();
       public static Cursor getImageCursor(Class<?> clazz, String path)
       {
         Image img = getImage(clazz, path);
         
         Cursor cursor = m_imageToCursorMap.get(img);
         if (cursor == null) {
                 cursor = new Cursor(Display.getDefault(), img.getImageData(),0,0);
                 m_imageToCursorMap.put(img, cursor);
         }
         return cursor;
       }
       
       
       /**
        * Dispose all of the cached cursors.
        */
       public static void disposeCursors() {
               for (Cursor cursor : m_idToCursorMap.values()) {
                       cursor.dispose();
               }
               m_idToCursorMap.clear();
               for (Cursor cursor : m_imageToCursorMap.values()) {
                 cursor.dispose();
               }
               m_imageToCursorMap.clear();
       }
       ////////////////////////////////////////////////////////////////////////////
       //
       // General
       //
       ////////////////////////////////////////////////////////////////////////////
       /**
        * Dispose of cached objects and their underlying OS resources. This should only be called when the cached
        * objects are no longer needed (e.g. on application shutdown).
        */
       public static void dispose() {
               disposeColors();
               disposeImages();
               disposeFonts();
               disposeCursors();
       }
   

      public static Color getDarkColor(RGB rgb) {
        return getDarkColor(getColor(rgb));
      }
}
