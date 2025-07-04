
package com.esotericsoftware.color.space;

import static com.esotericsoftware.color.Util.*;

import com.esotericsoftware.color.Illuminant;
import com.esotericsoftware.color.Util;
import com.esotericsoftware.color.space.LMS.CAT;

/** CIE 1931 tristimulus values. Foundation of colorimetry. */
public record XYZ (
	/** X tristimulus [0+]. */
	float X,
	/** Y tristimulus (luminance) [0+]. */
	float Y,
	/** Z tristimulus [0+]. */
	float Z) implements Color {

	/** Planck constant. **/
	static public double h = 6.62607015e-34;
	/** Speed of light in m/s. **/
	static public double c = 299792458;
	/** Boltzmann constant. **/
	static public double k = 1.380649e-23;
	/** First radiation constant. **/
	static public double c1 = (float)(2.0 * Math.PI * h * c * c);
	/** Second radiation constant. **/
	static public double c2 = (float)(h * c / k);
	/** Maximum luminous efficacy (lm/W) */
	static public final float Km = 683;

	public float get (int index) {
		return switch (index) {
		case 0 -> X;
		case 1 -> Y;
		case 2 -> Z;
		default -> throw new IndexOutOfBoundsException(index);
		};
	}

	public XYZ set (int index, float value) {
		return switch (index) {
		case 0 -> new XYZ(value, Y, Z);
		case 1 -> new XYZ(X, value, Z);
		case 2 -> new XYZ(X, Y, value);
		default -> throw new IndexOutOfBoundsException(index);
		};
	}

	public CAM02 CAM02 (CAM02.VC vc) {
		float rT = X * 0.7328f + Y * 0.4296f + Z * -0.1624f; // CAT02.
		float gT = X * -0.7036f + Y * 1.6975f + Z * 0.0061f;
		float bT = X * 0.0030f + Y * 0.0136f + Z * 0.9834f;
		float rC = vc.rgbD()[0] * rT, gC = vc.rgbD()[1] * gT, bC = vc.rgbD()[2] * bT; // Chromatic adaptation.
		float rP = rC * 0.7409791f + gC * 0.21802516f + bC * 0.04100575f; // HPE.
		float gP = rC * 0.28535329f + gC * 0.62420157f + bC * 0.09044513f;
		float bP = rC * -0.00962761f + gC * -0.00569803f + bC * 1.01532564f;
		float rAF = (float)Math.pow(vc.FL() * Math.abs(rP) / 100, 0.42); // Post-adaptation non-linear response compression.
		float gAF = (float)Math.pow(vc.FL() * Math.abs(gP) / 100, 0.42);
		float bAF = (float)Math.pow(vc.FL() * Math.abs(bP) / 100, 0.42);
		float rA = Math.signum(rP) * 400 * rAF / (rAF + 27.13f) + 0.1f;
		float gA = Math.signum(gP) * 400 * gAF / (gAF + 27.13f) + 0.1f;
		float bA = Math.signum(bP) * 400 * bAF / (bAF + 27.13f) + 0.1f;
		float a = (11 * rA + -12 * gA + bA) / 11, b = (rA + gA - 2 * bA) / 9; // Opponent color dimensions.
		float h = (float)Math.atan2(b, a) * radDeg;
		h = h < 0 ? h + 360 : h;
		float hRad = h * degRad, eT = 0.25f * ((float)Math.cos(hRad + 2) + 3.8f); // Eccentricity factor.
		float A = (2 * rA + gA + 0.05f * bA) * vc.Nbb(); // Achromatic response.
		// Lightness, signed power to handle negative A values.
		float J = 100 * Math.signum(A) * (float)Math.pow(Math.abs(A) / vc.Aw(), vc.c() * vc.z());
		float t = (50000 / 13f * vc.Nc() * vc.Ncb() * eT * (float)Math.sqrt(a * a + b * b)) / (rA + gA + 21 / 20f * bA); // Chroma.
		float C = J == 0 ? 0
			: (float)Math.pow(t, 0.9) * (float)Math.sqrt(Math.abs(J) / 100) * (float)Math.pow(1.64 - Math.pow(0.29, vc.n()), 0.73);
		float Q = J == 0 ? 0 : 4 / vc.c() * (float)Math.sqrt(Math.abs(J) / 100) * (vc.Aw() + 4) * vc.FLRoot(); // Brightness.
		float M = C * vc.FLRoot(); // Colorfulness.
		float s = (M == 0 || Q == 0) ? 0 : 100 * (float)Math.sqrt(Math.abs(M / Q)); // Saturation.
		return new CAM02(J, C, h, Q, M, s);
	}

	public CAM16 CAM16 (CAM16.VC vc) {
		float rT = (X * 0.401288f) + (Y * 0.650173f) + (Z * -0.051461f); // To cone/RGB responses.
		float gT = (X * -0.250268f) + (Y * 1.204414f) + (Z * 0.045854f);
		float bT = (X * -0.002079f) + (Y * 0.048952f) + (Z * 0.953127f);
		float rD = vc.rgbD()[0] * rT; // Discount illuminant.
		float gD = vc.rgbD()[1] * gT;
		float bD = vc.rgbD()[2] * bT;
		float rAF = (float)Math.pow(vc.FL() * Math.abs(rD) / 100, 0.42); // Chromatic adaptation.
		float gAF = (float)Math.pow(vc.FL() * Math.abs(gD) / 100, 0.42);
		float bAF = (float)Math.pow(vc.FL() * Math.abs(bD) / 100, 0.42);
		float rA = Math.signum(rD) * 400 * rAF / (rAF + 27.13f);
		float gA = Math.signum(gD) * 400 * gAF / (gAF + 27.13f);
		float bA = Math.signum(bD) * 400 * bAF / (bAF + 27.13f);
		float a = (11 * rA + -12 * gA + bA) / 11; // Redness-greenness.
		float b = (rA + gA - 2 * bA) / 9; // Yellowness-blueness.
		float u = (20 * rA + 20 * gA + 21 * bA) / 20; // Auxiliary components.
		float p2 = (40 * rA + 20 * gA + bA) / 20;
		float hDeg = (float)Math.atan2(b, a) * radDeg; // Hue.
		float h = hDeg < 0 ? hDeg + 360 : hDeg >= 360 ? hDeg - 360 : hDeg;
		float ac = p2 * vc.Nbb(); // Achromatic response to color.
		float J = 100 * (float)Math.pow(ac / vc.Aw(), vc.c() * vc.z()); // CAM16 lightness and brightness.
		float huePrime = (h < 20.14f) ? h + 360 : h; // CAM16 chroma, colorfulness, and saturation.
		float eHue = 0.25f * ((float)Math.cos(huePrime * degRad + 2) + 3.8f);
		float p1 = 50000 / 13f * eHue * vc.Nc() * vc.Ncb();
		float t = p1 * (float)Math.sqrt(a * a + b * b) / (u + 0.305f);
		float alpha = (float)Math.pow(1.64 - Math.pow(0.29, vc.n()), 0.73) * (float)Math.pow(t, 0.9);
		float C = alpha * (float)Math.sqrt(J / 100); // CAM16 chroma, colorfulness, saturation.
		return new CAM16(J, C, h, 4 / vc.c() * (float)Math.sqrt(J / 100) * (vc.Aw() + 4) * vc.FLRoot(), C * vc.FLRoot(),
			50 * (float)Math.sqrt((alpha * vc.c()) / (vc.Aw() + 4)));
	}

	/** @return NaN if invalid. */
	public HunterLab HunterLab () {
		if (Y < EPSILON) return new HunterLab(0, 0, 0);
		float sqrt = (float)Math.sqrt(Y);
		return new HunterLab(10 * sqrt, 17.5f * ((1.02f * X - Y) / sqrt), 7 * ((Y - 0.847f * Z) / sqrt));
	}

	/** @param whitePoint See {@link Illuminant}. */
	public Lab Lab (XYZ whitePoint) {
		float X = this.X / whitePoint.X, Y = this.Y / whitePoint.Y, Z = this.Z / whitePoint.Z;
		X = X > Lab.e ? (float)Math.pow(X, 1 / 3d) : (Lab.k * X + 16) / 116;
		Y = Y > Lab.e ? (float)Math.pow(Y, 1 / 3d) : (Lab.k * Y + 16) / 116;
		Z = Z > Lab.e ? (float)Math.pow(Z, 1 / 3d) : (Lab.k * Z + 16) / 116;
		return new Lab(116 * Y - 16, 500 * (X - Y), 200 * (Y - Z));
	}

	public LinearRGB LinearRGB () {
		float X = this.X / 100, Y = this.Y / 100, Z = this.Z / 100;
		return new LinearRGB( //
			3.2404542f * X - 1.5371385f * Y - 0.4985314f * Z, //
			-0.969266f * X + 1.8760108f * Y + 0.041556f * Z, //
			0.0556434f * X - 0.2040259f * Y + 1.0572252f * Z);
	}

	public LMS LMS (CAT matrix) {
		float[] lms = matrixMultiply(X, Y, Z, matrix.forward);
		return new LMS(lms[0], lms[1], lms[2]);
	}

	/** @return NaN if invalid. */
	public Luv Luv (XYZ whitePoint) {
		float Xn = whitePoint.X, Yn = whitePoint.Y, Zn = whitePoint.Z;
		float yr = Y / Yn, L = yr > Lab.e ? 116 * (float)Math.cbrt(yr) - 16 : Lab.k * yr;
		float divisor = X + 15 * Y + 3 * Z, divisorN = Xn + 15 * Yn + 3 * Zn;
		if (divisor < EPSILON || divisorN < EPSILON) return new Luv(L, Float.NaN, Float.NaN);
		float u_prime = 4 * X / divisor, v_prime = 9 * Y / divisor;
		float un_prime = 4 * Xn / divisorN, vn_prime = 9 * Yn / divisorN;
		return new Luv(L, 13 * L * (u_prime - un_prime), 13 * L * (v_prime - vn_prime));
	}

	public RGB RGB () {
		float X = this.X / 100, Y = this.Y / 100, Z = this.Z / 100;
		return new RGB(sRGB(clamp(3.2404542f * X - 1.5371385f * Y - 0.4985314f * Z)),
			sRGB(clamp(-0.969266f * X + 1.8760108f * Y + 0.041556f * Z)),
			sRGB(clamp(0.0556434f * X - 0.2040259f * Y + 1.0572252f * Z)));
	}

	/** @return NaN if invalid. */
	public uv uv () {
		float denom = X + 15 * Y + 3 * Z;
		return new uv(4 * X / denom, 9 * Y / denom);
	}

	/** @return NaN if invalid. */
	public uv1960 uv1960 () {
		float denom = X + 15 * Y + 3 * Z;
		return new uv1960(4 * X / denom, 6 * Y / denom);
	}

	public UVW UVW (XYZ whitePoint) {
		float yRatio = Y / whitePoint.Y();
		if (yRatio <= 0) return new UVW(0, 0, -17);
		float denom = X() + 15 * Y + 3 * Z;
		float denomWhite = whitePoint.X() + 15 * whitePoint.Y() + 3 * whitePoint.Z();
		if (denom < EPSILON || denomWhite < EPSILON) return new UVW(0, 0, 0);
		float u = 4 * X / denom, v = 9 * Y / denom;
		float un = 4 * whitePoint.X() / denomWhite;
		float vn = 9 * whitePoint.Y() / denomWhite;
		float W = 25 * (float)Math.pow(yRatio, 1 / 3f) - 17;
		return new UVW(13 * W * (u - un), 13 * W * (v - vn), W);
	}

	/** @return NaN if invalid. */
	public xy xy () {
		float sum = X + Y + Z;
		if (sum < EPSILON) return new xy(Float.NaN, Float.NaN);
		return new xy(X / sum, Y / sum);
	}

	/** @return NaN if invalid. */
	public xyY xyY () {
		float sum = X + Y + Z;
		if (sum < EPSILON) return new xyY(Float.NaN, Float.NaN, Float.NaN);
		return new xyY(X / sum, Y / sum, Y);
	}

	public XYZ add (float value) {
		return new XYZ(X + value, Y + value, Z + value);
	}

	public XYZ add (int index, float value) {
		return switch (index) {
		case 0 -> new XYZ(X + value, Y, Z);
		case 1 -> new XYZ(X, Y + value, Z);
		case 2 -> new XYZ(X, Y, Z + value);
		default -> throw new IndexOutOfBoundsException(index);
		};
	}

	public XYZ add (float X, float Y, float Z) {
		return new XYZ(this.X + X, this.Y + Y, this.Z + Z);
	}

	/** Uses {@link LMS.CAT#Bradford}. */
	public XYZ chromaticAdaptation (XYZ sourceIlluminant, XYZ destIlluminant) {
		return chromaticAdaptation(sourceIlluminant, destIlluminant, LMS.CAT.Bradford);
	}

	/** @return This color adapted to appear under the target illuminant as it would under the source illuminant. */
	public XYZ chromaticAdaptation (XYZ sourceIlluminant, XYZ targetIlluminant, LMS.CAT transform) {
		LMS lms = LMS(transform), sourceLMS = sourceIlluminant.LMS(transform), targetLMS = targetIlluminant.LMS(transform);
		return new LMS(lms.L() * targetLMS.L() / sourceLMS.L(), //
			lms.M() * targetLMS.M() / sourceLMS.M(), //
			lms.S() * targetLMS.S() / sourceLMS.S()).XYZ(transform);
	}

	public XYZ lerp (XYZ other, float t) {
		return new XYZ(Util.lerp(X, other.X, t), Util.lerp(Y, other.Y, t), Util.lerp(Z, other.Z, t));
	}

	public float max () {
		return Util.max(X, Y, Z);
	}

	public float min () {
		return Util.min(X, Y, Z);
	}

	public XYZ norY () {
		if (Y < EPSILON) return new XYZ(Float.NaN, Float.NaN, Float.NaN);
		float normalize = 100 / Y;
		return new XYZ(X * normalize, 100, Z * normalize);
	}

	public XYZ norMax () {
		float max = max();
		return max < EPSILON ? this : new XYZ(X / max, Y / max, Z / max);
	}

	public XYZ sub (float value) {
		return new XYZ(X - value, Y - value, Z - value);
	}

	public XYZ sub (int index, float value) {
		return switch (index) {
		case 0 -> new XYZ(X - value, Y, Z);
		case 1 -> new XYZ(X, Y - value, Z);
		case 2 -> new XYZ(X, Y, Z - value);
		default -> throw new IndexOutOfBoundsException(index);
		};
	}

	public XYZ sub (float X, float Y, float Z) {
		return new XYZ(this.X - X, this.Y - Y, this.Z - Z);
	}

	public XYZ scl (float value) {
		return new XYZ(X * value, Y * value, Z * value);
	}

	public XYZ scl (int index, float value) {
		return switch (index) {
		case 0 -> new XYZ(X * value, Y, Z);
		case 1 -> new XYZ(X, Y * value, Z);
		case 2 -> new XYZ(X, Y, Z * value);
		default -> throw new IndexOutOfBoundsException(index);
		};
	}

	public XYZ scl (float X, float Y, float Z) {
		return new XYZ(this.X * X, this.Y * Y, this.Z * Z);
	}

	public float dst (XYZ other) {
		return (float)Math.sqrt(dst2(other));
	}

	public float dst2 (XYZ other) {
		float dx = X - other.X, dy = Y - other.Y, dz = Z - other.Z;
		return dx * dx + dy * dy + dz * dz;
	}

	public float len () {
		return (float)Math.sqrt(len2());
	}

	public float len2 () {
		return X * X + Y * Y + Z * Z;
	}

	public XYZ withY (float Y) {
		return set(1, Y);
	}

	@SuppressWarnings("all")
	public XYZ XYZ () {
		return this;
	}

	/** CIE 1931 x-bar color matching function (380-780nm at 5nm intervals). **/
	static public float[] Xbar = {0.001368f, 0.002236f, 0.004243f, 0.00765f, 0.01431f, 0.02319f, 0.04351f, 0.07763f, 0.13438f,
		0.21477f, 0.2839f, 0.3285f, 0.34828f, 0.34806f, 0.3362f, 0.3187f, 0.2908f, 0.2511f, 0.19536f, 0.1421f, 0.09564f,
		0.05795001f, 0.03201f, 0.0147f, 0.0049f, 0.0024f, 0.0093f, 0.0291f, 0.06327f, 0.1096f, 0.1655f, 0.2257499f, 0.2904f,
		0.3597f, 0.4334499f, 0.5120501f, 0.5945f, 0.6784f, 0.7621f, 0.8425f, 0.9163f, 0.9786f, 1.0263f, 1.0567f, 1.0622f, 1.0456f,
		1.0026f, 0.9384f, 0.8544499f, 0.7514f, 0.6424f, 0.5419f, 0.4479f, 0.3608f, 0.2835f, 0.2187f, 0.1649f, 0.1212f, 0.0874f,
		0.0636f, 0.04677f, 0.0329f, 0.0227f, 0.01584f, 0.01135916f, 0.008110916f, 0.005790346f, 0.004109457f, 0.002899327f,
		0.00204919f, 0.001439971f, 0.000999949f, 0.000690079f, 0.000476021f, 0.000332301f, 0.000234826f, 0.000166151f, 0.000117413f,
		0.000083075f, 0.000058707f, 0.00004151f};

	/** CIE 1931 y-bar color matching function (380-780nm at 5nm intervals). **/
	static public float[] Ybar = {0.000039f, 0.000064f, 0.00012f, 0.000217f, 0.000396f, 0.00064f, 0.00121f, 0.00218f, 0.004f,
		0.0073f, 0.0116f, 0.01684f, 0.023f, 0.0298f, 0.038f, 0.048f, 0.06f, 0.0739f, 0.09098f, 0.1126f, 0.13902f, 0.1693f, 0.20802f,
		0.2586f, 0.323f, 0.4073f, 0.503f, 0.6082f, 0.71f, 0.7932f, 0.862f, 0.9148501f, 0.954f, 0.9803f, 0.9949501f, 1, 0.995f,
		0.9786f, 0.952f, 0.9154f, 0.87f, 0.8163f, 0.757f, 0.6949f, 0.631f, 0.5668f, 0.503f, 0.4412f, 0.381f, 0.321f, 0.265f, 0.217f,
		0.175f, 0.1382f, 0.107f, 0.0816f, 0.061f, 0.04458f, 0.032f, 0.0232f, 0.017f, 0.01192f, 0.00821f, 0.005723f, 0.004102f,
		0.002929f, 0.002091f, 0.001484f, 0.001047f, 0.00074f, 0.00052f, 0.0003611f, 0.0002492f, 0.0001719f, 0.00012f, 0.0000848f,
		0.00006f, 0.0000424f, 0.00003f, 0.0000212f, 0.00001499f};

	/** Ybar sum * 5nm. */
	static public float YbarIntegral = 106.85664f;

	/** CIE 1931 z-bar color matching function (380-780nm at 5nm intervals). **/
	static public float[] Zbar = {0.006450001f, 0.01054999f, 0.02005001f, 0.03621f, 0.06785001f, 0.1102f, 0.2074f, 0.3713f,
		0.6456f, 1.0390501f, 1.3856f, 1.62296f, 1.74706f, 1.7826f, 1.77211f, 1.7441f, 1.6692f, 1.5281f, 1.28764f, 1.0419f,
		0.8129501f, 0.6162f, 0.46518f, 0.3533f, 0.272f, 0.2123f, 0.1582f, 0.1117f, 0.07824999f, 0.05725001f, 0.04216f, 0.02984f,
		0.0203f, 0.0134f, 0.008749999f, 0.005749999f, 0.0039f, 0.002749999f, 0.0021f, 0.0018f, 0.001650001f, 0.0014f, 0.0011f,
		0.001f, 0.0008f, 0.0006f, 0.00034f, 0.00024f, 0.00019f, 0.0001f, 0.00005f, 0.00003f, 0.00002f, 0.00001f, 0, 0, 0, 0, 0, 0,
		0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
}
