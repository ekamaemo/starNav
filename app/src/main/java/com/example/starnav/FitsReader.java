package com.example.starnav;
import nom.tam.fits.Fits;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.TableHDU;
import nom.tam.util.Cursor;

public class FitsReader {
    public float getPolarisY(String pathFits) {
        String fitsFilePath = pathFits; // Путь к вашему FITS-файлу
        double polarRa = 37.95456067;  // RA Полярной звезды (в градусах, J2000)
        double polarDec = 89.26410897; // DEC Полярной звезды (в градусах, J2000)
        double tolerance = 5;       // Допустимая погрешность в градусах

        try {
            Fits fits = new Fits(fitsFilePath);
            TableHDU hdu = (TableHDU) fits.getHDU(1); // Предполагаем, что данные во 2-м HDU (индекс 1)

            // Получаем колонки RA, DEC, X, Y
            double[] raArr = (double[]) hdu.getColumn("ra");
            double[] decArr = (double[]) hdu.getColumn("dec");
            float[] xArr = (float[]) hdu.getColumn("x");
            float[] yArr = (float[]) hdu.getColumn("y");

            // Поиск Полярной звезды
            for (int i = 0; i < raArr.length; i++) {
                if (Math.abs(raArr[i] - polarRa) <= tolerance &&
                        Math.abs(decArr[i] - polarDec) <= tolerance) {
                    System.out.printf("Полярная звезда найдена: x=%.2f, y=%.2f%n", xArr[i], yArr[i]);
                    return yArr[i];
                }
            }
            System.out.println("Полярная звезда не найдена в файле.");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}