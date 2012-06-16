class fft{
	int iTap;
	final double dWpi = 2 * Math.PI;
	
	boolean fastFt(double dData[],double dImg[],double dPower[],double dPhase[],boolean isRev){
		double sc,f,c,s,t,c1,s1,x1,kyo1;
		double dHan,dDum;
		int n,n1,j,i,k,ns,l1,i0,i1;
		int iInt;

		if(!isPowerOfTwo(iTap))
			return false;
		if(!isRev){
			for(iInt = 0;iInt < iTap;iInt ++)
			{
				dImg[iInt] = 0;														// Imaginary part 
				dHan = (1 - Math.cos((dWpi * (double)iInt) / (double)iTap)) / 2;	// Hanning Window 
				dData[iInt] *= dHan;												// Real part 
			}
		}	
		/*	printf("******************** Arranging BIT ******************\n"); */

		n = iTap;	/* NUMBER of DATA */
		n1 = n / 2;
		sc = Math.PI;
		j = 0;
		for(i = 0;i < n - 1;i ++)
		{
			if(i <= j)
			{
				t = dData[i];  dData[i] = dData[j];  dData[j] = t;
				t = dImg[i];   dImg[i] = dImg[j];   dImg[j] = t;
			}
			k = n / 2;
			while(k <= j)
			{
				j = j - k;
				k /= 2;
			}
			j += k;
		}
		/*	printf("******************** MAIN LOOP **********************\n"); */
		ns = 1;
		if(isRev)															// reverse
			f = 1.0;
		else
			f = -1.0;
		while(ns <= n / 2)
		{
			c1 = (double)Math.cos(sc);
			s1 = (double)Math.sin(f * sc);
			c = 1.0;
			s = 0.0;
			for(l1 = 0;l1 < ns;l1 ++)
			{
				for(i0 = l1;i0 < n;i0 += (2 * ns))
				{
					i1 = i0 + ns;
					x1 = (dData[i1] * c) - (dImg[i1] * s);
					kyo1 = (dImg[i1] * c) + (dData[i1] * s);
					dData[i1] = dData[i0] - x1;
					dImg[i1] = dImg[i0] - kyo1;
					dData[i0] = dData[i0] + x1;
					dImg[i0] = dImg[i0] + kyo1;
				}
				t = (c1 * c) - (s1 * s);
				s = (s1 * c) + (c1 * s);
				c = t;
			}
			ns *= 2;
			sc /= 2.0;
		}
		if(!isRev)
		{
			for(iInt = 0;iInt < iTap;iInt ++)
			{
				dData[iInt] /= (double)iTap;
				dImg[iInt] /= (double)iTap;
				dPower[iInt] = Math.sqrt(dData[iInt] * dData[iInt] + dImg[iInt] * dImg[iInt]);
				dPhase[iInt] = Math.atan2(dImg[iInt],dData[iInt]);
			}
		}
		return true;
	}

	public static boolean isPowerOfTwo(int x)
	{  

		return x > 0 && (x & (x - 1)) == 0;  

	}
}
