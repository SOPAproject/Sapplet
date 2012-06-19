/*
 *	SappletPanther.java
 *
 */

import java.io.*;
import java.io.IOException;
import java.util.*;
import java.net.MalformedURLException; 
import java.net.URL;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/*
 * <APPLET CODE="SappletPanther.class" WIDTH=800 HEIGHT=400>
 * </APPLET>
 */


class largeImage
{
	static Image img;
	static short sXpos = 0;
	static short width;
	static short frmWidth = 800;
}

public class SappletPanther extends Applet implements Runnable 
{
	Thread thread = null;
	private static final int	EXTERNAL_BUFFER_SIZE = 32768;
	String strFilename;
	int	x = -1,y,nMsg = 0;
	int nRot = 0;
	int nSampleRate = 44100;
	largeImage img;

	public void init()
	{
		strFilename = "panther22k.sopa";

		thread = new Thread(this);
		thread.start();
		largeImage.img = getImage(getDocumentBase(),"inst_panther.gif");
		addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent me)
			{
				x = me.getX();
				y = me.getY();
				if(nMsg == 2 || nMsg == -2)
				{
					nMsg = -1;
					repaint();
				}
				else if(nMsg != -1)
				{
					if(x >= largeImage.frmWidth * 3 / 4)
					{
						largeImage.sXpos -= largeImage.width / 36;
						nRot -= 2;
						if(nRot <= -36)
							nRot += 72;
						repaint();
					}
					else if(x < largeImage.frmWidth / 4)
					{
						largeImage.sXpos += largeImage.width / 36;
						nRot += 2;
						if(nRot > 36)
							nRot -= 72;
						repaint();
					}
					else
						nMsg ++;
				}
			}
		});
	}

	public void paint(Graphics g)
	{	
		largeImage.width = (short)largeImage.img.getWidth(this);
		g.drawImage(largeImage.img,largeImage.sXpos,120,this);
		if(largeImage.sXpos + largeImage.width < largeImage.frmWidth)
		{
			largeImage.sXpos += largeImage.width;
			g.drawImage(largeImage.img,largeImage.sXpos,120,this);
		}
		if(largeImage.sXpos > 0)
		{
			largeImage.sXpos -= largeImage.width;
			g.drawImage(largeImage.img,largeImage.sXpos,120,this);
		}
		g.drawImage(largeImage.img,largeImage.sXpos,120,this);

		if(nMsg == 1)
		{
			g.drawString("Playing " + strFilename, 120,40);
			g.drawString("If you want to stop, click here.", 200,80);
		}
		else if(nMsg == -2)
		{
			g.drawString(strFilename + " was played.", 120,40);
			g.drawString("Thank you.", 120,80);
			g.drawImage(largeImage.img,largeImage.sXpos,120,this);
		}
		else if(nMsg == 2)
		{
			g.drawString("Sound stopped by user.", 120,40);
		}
		else if(nMsg == 0)
		{
			g.drawString("Click here to start.", 240,40);
		}
		else if(nMsg == -3)
		{
			g.drawString("Reading file error!.", 120,40);
		}
		else
		{
			g.drawString("If you want to replay this demonstration,", 120,40);
			g.drawString("please refresh this page.", 120,60);
		}
		g.drawString("< -- Pan to the left", 20,320);
		g.drawString("Pan to the Right -- >", 650,320);
		g.drawString("Please use stereo headphones.", 120,360);
	}

	public void run()
	{
		int nNum = 0;
		int nChannels = 2;
		int nBit = 16;
		int nOverlap = 4;
		int nCnt;
		int[] nByte = new int[4];
		int[] nFmt = new int[3];
		int nTerm0[] = {82,73,70,70};				// RIFF
		int nTerm1[] = {83,79,80,65};				// SOPA
		int nTerm2[] = {102,109,116};				// fmt
		short[] sHrtf = new short[36864];
		short[] sPhase = new short[36864];
//		int iAv;
		InputStream inStream = null;
		AudioFormat audioFormat = null;	
		DataInputStream din = null;

		while(nMsg == 0)
		{
			try
			{
				Thread.sleep(1);
			}
			catch(InterruptedException e){}
		}
		repaint();

		
		// Prepair HRTF (level) database
		try
		{
//			din = new DataInputStream(new FileInputStream("hrtf512.bin"));
			InputStream hrtfStr = new URL(getDocumentBase(),"hrtf512.bin").openStream();
			din = new DataInputStream(hrtfStr);
			for(nNum = 0;nNum < 36864;nNum ++) 
			{															// BigEndian to LittleEndian
				sHrtf[nNum] = (short)((din.readByte() & 0xff) + (din.readByte() << 8));
			}
			din.close();
		}
		catch(Exception e)
		{
//			System.out.println("HRTF data error! : " + e);
			nMsg = -8;
		}

		// Prepair HRTF (phase) database
		try
		{
//			din = new DataInputStream(new FileInputStream("phase512.bin"));
			InputStream hrtfStr = new URL(getDocumentBase(),"phase512.bin").openStream();
			din = new DataInputStream(hrtfStr);
			for(nNum = 0;nNum < 36864;nNum ++) 
			{															// BigEndian to LittleEndian
				sPhase[nNum] = (short)((din.readByte() & 0xff) + (din.readByte() << 8));
			}
			din.close();
		}
		catch(Exception e)
		{
//			System.out.println("PHASE data error! : " + e);
			nMsg = -8;
		}
//		System.out.println("HRTF data number " + nNum);

// Open SOPA (Spatial Audio File)
		URL url = null;
		if(nMsg != -8)
		{
			if(nMsg >= 0)
			{
				try
				{
					inStream =  new URL(getDocumentBase(),"panther22k.sopa").openStream();
					for(nCnt = 0;nCnt < 4;nCnt ++)
					{
						nByte[nCnt] = inStream.read();
						if(nByte[nCnt] == -1)
						{
							inStream.close();
							System.exit(1);
						}
					}
					if(!Arrays.equals(nByte,nTerm0))
					{
//						System.out.println("File format error!");
						inStream.close();
						System.exit(1);
					}
//					System.out.println("RIFF OK");
					for(nCnt = 0;nCnt < 4;nCnt ++)
					{
						nByte[nCnt] = inStream.read();
						if(nByte[nCnt] == -1)
						{
							inStream.close();
							System.exit(1);
						}
					}
					for(nCnt = 0;nCnt < 4;nCnt ++)
					{
						nByte[nCnt] = inStream.read();
						if(nByte[nCnt] == -1)
						{
							inStream.close();
							System.exit(1);
						}
					}
					if(!Arrays.equals(nByte,nTerm1))
					{
//						System.out.println("File format error!");
						inStream.close();
						System.exit(1);
					}
//					System.out.println("SOPA OK");
					for(nCnt = 0;nCnt < 3;nCnt ++)
					{
						nFmt[nCnt] = inStream.read();
						if(nFmt[nCnt] == -1)
						{
							inStream.close();
							System.exit(1);
						}
					}
					if(!Arrays.equals(nFmt,nTerm2))
					{
//						System.out.println("File format error!");
						inStream.close();
						System.exit(1);
					}
//					System.out.println("fmt OK");
					inStream.read();				
			
					nBit = inStream.read();
					if(nBit != 16)
					{
//						System.out.println("Data are not 16-bit!");
						inStream.close();
						System.exit(1);
					}
					for(nCnt = 0;nCnt < 3;nCnt ++)
						inStream.read();
					if(inStream.read() != 1)
					{
//						System.out.println("Data are not PCM!");
						inStream.close();
						System.exit(1);
					}
					inStream.read();
					nOverlap = inStream.read();
					if(nOverlap != 2 && nOverlap != 4)
					{
//						System.out.println("Wrong value (nOverlap)!");
						inStream.close();
						System.exit(1);
					}
					inStream.read();
					nSampleRate = inStream.read();
					nSampleRate += inStream.read() * 256;
					for(nCnt = 0;nCnt < 10;nCnt ++)
					{
						inStream.read();
					}
					for(nCnt = 0;nCnt < 4;nCnt ++)
					{
						nByte[nCnt] = inStream.read();
						if(nByte[nCnt] == -1)
						{
							inStream.close();
							System.exit(1);
						}
					}
//					System.out.println("SOPA file version = " + nByte[3] + "." + nByte[2] + "." + nByte[1] + "." + nByte[0]);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					nMsg = -3;
				}
			}
		}

		if(nMsg >= 0)
		{
			audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,nSampleRate,nBit,nChannels,
				nChannels * nBit / 8,nSampleRate,false);

			SourceDataLine	line = null;
			DataLine.Info	info = new DataLine.Info(SourceDataLine.class,
				audioFormat);

			try
			{
				line = (SourceDataLine) AudioSystem.getLine(info);

				line.open(audioFormat);
			}
			catch (LineUnavailableException e)
			{
				e.printStackTrace();
				nMsg = -2;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				nMsg = -2;
			}
			line.start();

			// Signal processing
			final int iFIN = 16384;
			final int iBYTE = nBit / 8;
			final int iSection = EXTERNAL_BUFFER_SIZE / iBYTE / nChannels;
			int iSect,iCount,iInt,nSamplesWritten,iNumb,iNumImage,iTmp;
			int iRatio = 44100 / nSampleRate;
			int iSize = 1024;								// FFT window size
			int iHlf = 512;
			int iFnum;										// number of frames
			int iRead = 0;
			int iProc = iSize / nOverlap;
			int iRem = iSize - iProc;
			int	nBytesRead = 0;
			int nBytesWritten = 0;
			byte[]	abData = new byte[EXTERNAL_BUFFER_SIZE];
			byte[] bRet = new byte[2];
			int[]	iAngl = new int[iSize];
			short[][] sData = new short[2][EXTERNAL_BUFFER_SIZE / 2];
			short[][] sVal = new short[2][EXTERNAL_BUFFER_SIZE / 4];
			short sDum[][] = new short[2][iFIN];
			short sSample,sTmp;
			double dSpL,dSpR,dSpImageL,dSpImageR,dtemp;
			double dTmp,dPhaseL,dPhaseImageL,dPhaseR,dPhaseImageR;

			double dReL[] = new double[iSize + 1];
			double dImL[] = new double[iSize + 1];
			double dReR[] = new double[iSize + 1];
			double dImR[] = new double[iSize + 1];
			double dPow[] = new double[iSize + 1];
			double dPh[] = new double[iSize + 1];

			fft test = new fft();

			try
			{
				for(nCnt = 0;nCnt < 4;nCnt ++)
					inStream.read();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				System.exit(1);
			}
			nSamplesWritten = 0;

			//			iAv = line.available();
			//			System.out.println("Available buffer size = " + iAv);
			while(nMsg == 1)
			{
				try
				{
					nBytesRead = iRead = 0;
					iTmp = abData.length;
					while(iTmp > 0 && iRead >= 0)
					{
						iRead = inStream.read(abData, nBytesRead, iTmp);
						if(iRead == -1)
						{
							iTmp = 0;
						}
						else
						{
							nBytesRead += iRead;
							iTmp -= iRead;
						}
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				if(nBytesWritten == 0)
				{
					iInt = 5;
					sSample = 1;
					while(sSample > 0)
					{
						sSample = (short)abData[iInt];
						iInt += 4;
					}
					iSize = iInt - 5;
//					System.out.println("FFT window size = " + iSize);
					iProc = iSize / nOverlap;
					iRem = iSize - iProc;
					iHlf = iSize / 2;							// half of FFT window size
					test.iTap = iSize;							// FFT window size
					iRatio *= iSize / 512;
				}
				iSect = nBytesRead / iBYTE / nChannels;			// samples per section
				if(nBytesWritten > 0)
					iSect += iRem;
				iFnum = iSect / iProc;						// number of frames
				if(nBytesRead == EXTERNAL_BUFFER_SIZE)
					iFnum -= nOverlap - 1;

				dPow[iSize] = dPh[iSize] = 0;
				if(nBytesRead > 0)							// We have data to process
				{
					rearrangeSAF(abData,sVal,false);
					for(iInt = 0;iInt < iFnum;iInt ++)
					{
						iCount = iProc * iInt;
						if(nBytesWritten == 0)
						{
							for(nNum = 0;nNum < iSize;nNum ++)
							{
								dReR[nNum] = (double)sVal[1][iCount + nNum];		// signal in the right channel
							}
						}
						else
						{
							for(nNum = 0;nNum < iSize;nNum ++)
							{
								if(iCount + nNum < iRem)
									dReR[nNum] = (double)sDum[1][iCount + nNum];		
								else
									dReR[nNum] = (double)sVal[1][iCount + nNum - iRem];		// signal in the right channel
							}
						}
						if(test.fastFt(dReR,dImR,dPow,dPh,false))									// FFT
						{
							iAngl[iHlf] = 0;
							for(nNum = 0;nNum < iHlf;nNum ++)
							{
								int iBer = nNum / 2;
								int iFreq = nNum / iRatio;
								if(nBytesWritten == 0)
								{
									sTmp = sVal[0][iCount + iBer];
								}
								else if(iInt < nOverlap - 1)
								{
									sTmp = sDum[0][iCount + iBer];
								}	
								else
								{
									sTmp = sVal[0][iCount - iRem + iBer];
								}	
								if(nNum % 2 == 0)
								{
									if(sTmp < 0)
										iAngl[nNum] = -sTmp / 256;
									else
										iAngl[nNum] = sTmp / 256;
								}
								else
								{
									if(sTmp < 0)
										iAngl[nNum] = (-sTmp) % 256;
									else
										iAngl[nNum] = sTmp % 256;
								}
								if(iAngl[nNum] <= 0)
								{
									dSpR = dPow[nNum];
									dSpL = dPow[nNum];
									dSpImageL = dPow[iSize - nNum];
									dSpImageR = dPow[iSize - nNum];
									dPhaseL = dPh[nNum];
									dPhaseR = dPh[nNum];
									dPhaseImageL = dPh[iSize - nNum];
									dPhaseImageR = dPh[iSize - nNum];
								}
								else
								{
									iAngl[nNum] += nRot;
									iAngl[nNum] -= 1;
									//								iAngl[nNum] = 18;
									if(iAngl[nNum] > 71)
									{
										iAngl[nNum] -= 72;
									}
									else if(iAngl[nNum] < 0)
									{
										iAngl[nNum] += 72;
									}
									iNumb = 512 * (71 - iAngl[nNum]) + iFreq;
									iNumImage = 512 * (71 - iAngl[nNum]) + iSize - iFreq;
									if(iNumImage >= 36864)
										iNumImage -= 36864;
									else if(iNumImage < 0)
										iNumImage += 36864;
									if(iNumb >= 36864)
										iNumb -= 36864;
									else if(iNumb < 0)
										iNumb += 36864;

									dTmp = (double)sHrtf[iNumb];
									dSpL = dPow[nNum] * dTmp / 2048;
									dTmp = (double)sPhase[iNumb];
									dPhaseL = dPh[nNum] + dTmp / 10000.0;
									dTmp = (double)sHrtf[iNumImage];
									dSpImageL = dPow[iSize - nNum] * dTmp / 2048;
									dTmp = (double)sPhase[iNumImage];
									dPhaseImageL = dPh[iSize - nNum] + dTmp / 10000.0;

									iNumb = 512 * iAngl[nNum] + iFreq;
									iNumImage = 512 * iAngl[nNum] + iSize - iFreq;
									if(iNumImage >= 36864)
										iNumImage -= 36864;
									else if(iNumImage < 0)
										iNumImage += 36864;
									if(iNumb >= 36864)
										iNumb -= 36864;
									else if(iNumb < 0)
										iNumb += 36864;

									dTmp = (double)sHrtf[iNumb];
									dSpR = dPow[nNum] * dTmp / 2048;
									dTmp = (double)sPhase[iNumb];
									dPhaseR = dPh[nNum] + dTmp / 10000.0;
									dTmp = (double)sHrtf[iNumImage];
									dSpImageR = dPow[iSize - nNum] * dTmp / 2048;
									dTmp = (double)sPhase[iNumImage];
									dPhaseImageR = dPh[iSize - nNum] + dTmp / 10000.0;
								}
								dReL[nNum] = dSpL * Math.cos(dPhaseL);
								dReR[nNum] = dSpR * Math.cos(dPhaseR);
								dImL[nNum] = dSpL * Math.sin(dPhaseL);
								dImR[nNum] = dSpR * Math.sin(dPhaseR);
								dReL[iSize - nNum] = dSpImageL * Math.cos(dPhaseImageL);
								dReR[iSize - nNum] = dSpImageR * Math.cos(dPhaseImageR);
								dImL[iSize - nNum] = dSpImageL * Math.sin(dPhaseImageL);
								dImR[iSize - nNum] = dSpImageR * Math.sin(dPhaseImageR);
								/*								dRe[1][nNum] = dRe[0][nNum];
								 dIm[1][nNum] = dIm[0][nNum];
								 dRe[1][iSize - nNum] = dRe[0][iSize - nNum];
								 dIm[1][iSize - nNum] = dIm[0][iSize - nNum];	*/
							}
							dReL[iHlf] = dReR[iHlf];
							dImL[iHlf] = dImR[iHlf];
							if(test.fastFt(dReL,dImL,dPow,dPh,true))										// reverse FFT (left channel)
							{
								if(test.fastFt(dReR,dImR,dPow,dPh,true))									// reverse FFT (right channel)
								{
									for(nNum = 0;nNum < iSize;nNum ++)
									{
										dtemp = (1 - Math.cos(2 * Math.PI * (double)nNum / (double)iSize)) / 4;
										dReL[nNum] *= dtemp;
										dReR[nNum] *= dtemp;	
										sData[0][iCount + nNum] += dReL[nNum];
										sData[1][iCount + nNum] += dReR[nNum];
									}
								}
								//								else
								//									System.out.println("reverse FFT (right) Error\n");
							}
							//							else
							//								System.out.println("reverse FFT (left) Error\n");
						}
						//						else
						//							System.out.println(" forward FFT Error\n");
					}

					bRet[0] = bRet[1] = 0;
					for(iInt = 0;iInt < iSect - iRem;iInt ++)
					{
						intToByte((int)sData[0][iInt],bRet);
						abData[iInt * 4] = bRet[0];
						abData[iInt * 4 + 1] = bRet[1];
						intToByte((int)sData[1][iInt],bRet);
						abData[iInt * 4 + 2] = bRet[0];
						abData[iInt * 4 + 3] = bRet[1];	
					}
					iTmp = iSection - iRem;
					for(iInt = 0;iInt < iSect;iInt ++)
					{
						if(iInt < iRem)
						{
							if(nSamplesWritten == 0)
							{
								sData[0][iInt] = sData[0][iInt + iTmp];
								sData[1][iInt] = sData[1][iInt + iTmp];
							}
							else
							{
								sData[0][iInt] = sData[0][iInt + iTmp + iRem];
								sData[1][iInt] = sData[1][iInt + iTmp + iRem];
							}
							sDum[0][iInt] = sVal[0][iInt + iTmp];
							sDum[1][iInt] = sVal[1][iInt + iTmp];
						}
						else
						{
							sData[0][iInt] = sData[1][iInt] = 0;
						}
					}
					/*					iAv = line.available();
					 while(iAv < EXTERNAL_BUFFER_SIZE)
					 iAv = line.available();	*/
					if(nBytesRead < EXTERNAL_BUFFER_SIZE)
						nBytesWritten += line.write(abData, 0, nBytesRead + iRem * 4);
					else if(nBytesWritten != 0)
						nBytesWritten += line.write(abData, 0, nBytesRead);
					else
						nBytesWritten += line.write(abData, 0, nBytesRead - iRem * 4);
					nSamplesWritten = nBytesWritten / iBYTE / nChannels;
				}
				if(iRead < 0)
				{
					nMsg = -2;
					repaint();
				}
			}
//			System.out.println(nSamplesWritten + " samples were played.\n");

			line.drain();
			line.close();
			try
			{
				inStream.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.exit(1);
			}

			largeImage.sXpos = 0;
		}
		repaint();
	}

	private static void rearrangeSAF(byte bDt[], short sDt[][],boolean littleOrBig)
	{
		int nNum;
		int iTmp,iLow;
		int iV;

		for(nNum = 0;nNum < bDt.length;nNum += 2)
		{
			if(!littleOrBig)
			{
				iTmp = bDt[nNum + 1] << 8;
				iLow = bDt[nNum];
				iV = iTmp + (iLow & 0x000000FF);
			}
			else
			{
				iTmp = bDt[nNum] << 8;
				iLow = bDt[nNum + 1];
				iV = iTmp + (iLow & 0x000000FF);
			}
			if((nNum / 2) % 2 == 0)
			{
				sDt[0][nNum / 4] = (short)iV;						// information of direction
			}
			else
				sDt[1][(nNum - 2) / 4] = (short)iV;					// PCM data
		}
	}

	private static void intToByte(int iDt,byte bRet[])
	{
		int iV = iDt;

		bRet[0] = (byte)(iDt & 0x00FF);
		bRet[1] = (byte)(iDt >>> 8 & 0x00ff);
	}

	private static void out(String strMessage)
	{
		System.out.println(strMessage);
	}
}



/*** SappletPanther.java ***/

