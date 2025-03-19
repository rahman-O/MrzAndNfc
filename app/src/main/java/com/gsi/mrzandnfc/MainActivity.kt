package com.gsi.mrzandnfc




import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.gsi.mrzandnfc.ImageUtil.decodeImage
import com.gsi.mrzandnfc.smartscanner.ScannerScreen
import com.gsi.mrzandnfc.ui.theme.MrzAndNfcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sf.scuba.data.Gender
import net.sf.scuba.smartcards.CardService
import org.apache.commons.io.IOUtils
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1Set
import org.bouncycastle.asn1.x509.Certificate
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.SecurityInfo
import org.jmrtd.lds.icao.DG11File
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.icao.MRZInfo
import org.jmrtd.lds.iso19794.FaceImageInfo
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.util.Arrays
import java.util.regex.Matcher
import java.util.regex.Pattern
import android.view.View as View1


class MainActivity : ComponentActivity() {


    private var passportNumberFromIntent = false
    private var encodePhotoToBase64 = false
    private lateinit var mainLayout: View1
    private lateinit var isLoading: MutableState<Boolean>
    private lateinit var loadingLayout: View1

    private var mrzInfoForView: MRZInfo? = null

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            MrzAndNfcTheme {
                isLoading= remember { mutableStateOf(false) }
                NfcSecreen()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("ContextCastToActivity")
    @Composable
    private fun NfcSecreen() {
        var startScanner by rememberSaveable { mutableStateOf(false) }
        val textE = remember { mutableStateOf("") }
        var fileNfcImage by remember { mutableStateOf<Bitmap?>(null) }
        var fileNfcImage2 by remember { mutableStateOf<Bitmap?>(null) }
        var faceDetection by remember { mutableStateOf<Bitmap?>(null) }
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }


        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // عرض الصورة الأولى

            if (isLoading.value) {
                Text("جاري قراءة البيانات")
            }else{

                faceDetection?.let {
                    Image(
                        modifier = Modifier.size(150.dp),
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Face Detection"
                    )
                }


                fileNfcImage?.let {
                    Image(
                        modifier = Modifier.size(150.dp),
                        bitmap = it.asImageBitmap(),
                        contentDescription = "First Scan"
                    )
                }

                // عرض الصورة الثانية
                fileNfcImage2?.let {
                    Image(
                        modifier = Modifier.size(150.dp),
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Second Scan"
                    )
                }

                Text(text = textE.value)

                ElevatedButton(
                    onClick = { startScanner = true }
                ) {
                    Text("Scan")
                }
            }

        }

        // الكود المسؤول عن الماسح
        if (startScanner) {
            val context = LocalContext.current as Activity
            ScannerScreen(
                context = context,
                startScan = startScanner,
                onResult = { bitmap, imageUri ->
                    fileNfcImage = bitmap
                    coroutineScope.launch {
                        try {
                            faceDetection = FaceDetectionHelper().detectFace(bitmap, context)
                            if (faceDetection == null) {
                                textE.value = "الصورة لا تحتوي على وجه"
                            }

                        }catch (e: Exception){
                            e.printStackTrace()
                            Log.e("FaceDetection", "Error in face detection: ${e.message}")
                        }

                    }
                    // إذا لم يتم التقاط صورة بعد
                    /*if (fileNfcImage == null) {
                        fileNfcImage = bitmap
                        recognizeTextAndDetectLanguage(fileNfcImage!!) { recognizedText ->

                            if (recognizedText.contains("IDIRQ")) {

                                fileNfcImage = null
                                textE.value = "برجاء التقاط صورة للوجه الأمامي"
                                return@recognizeTextAndDetectLanguage
                            }else{
                                textE.value=""
                            }

                        }

                        // التعرف على النص للصورة الأولى


                    } else if (fileNfcImage2 == null&&fileNfcImage!=null) {



                        fileNfcImage2 = bitmap

                        // التعرف على النص للصورة الثانية
                        recognizeTextAndDetectLanguage(fileNfcImage2!!) { recognizedText ->
                            if (recognizedText.contains("IDIRQ").not()) {
                                Toast.makeText(context, "لم يتم التعرف على النص", Toast.LENGTH_SHORT).show()
                                fileNfcImage2 = null
                                textE.value = "برجاء التقاط صورة للوجه الخلفي"
                                return@recognizeTextAndDetectLanguage
                            }else{
                                val cleanText =cleanMrzText("IDIRQ"+recognizedText.substringAfter("IDIRQ"))

                                textE.value = cleanText
                                Log.d(TAG, "Second Image Text: $cleanText")

                                // محاولة تحليل الـ MRZ من النص المكتشف
                                handleMrzExtraction(cleanText)
                            }
                            }


                    } else {
                        Toast.makeText(context, "تم التقاط صورتين بالفعل", Toast.LENGTH_SHORT).show()
                    }*/

                    startScanner = false
                },
                onClosedActivity = {
                    startScanner = false
                }
            )
        }
    }

    // ---------------------- التعرف على النص ----------------------
    fun recognizeTextAndDetectLanguage(bitmap: Bitmap, onResult: (String) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (visionText.text.isNotEmpty()) {
                    onResult("تم التعرف على النص: ${visionText.text}")
                } else {
                    onResult("لم يتم التعرف على نص")
                }
            }
            .addOnFailureListener { e ->
                onResult("خطأ: ${e.message}")
            }
    }

    // ---------------------- استخراج بيانات MRZ ----------------------
    fun handleMrzExtraction(fullText: String) {
        val TYPE_IRAQI_ID = "IDIRQ"
        val IRAQI_ID_LINE_1_REGEX = "([A-Z]{5})([A-Z0-9]{8})(\\d)(\\d{12})<<<(\\d{7})([A-Z])(\\d{7})"
        val cleanedText = "IDIRQ" + fullText.substringAfter("IDIRQ").replace(" ", "").replace("\n", "")

        val patternIraqiId: Pattern = Pattern.compile(IRAQI_ID_LINE_1_REGEX)
        val matcherIraqiId: Matcher = patternIraqiId.matcher(cleanedText)

        if (matcherIraqiId.find()) {
            val iraqiIdText: String = cleanedText.substring(0, 5)
            if (iraqiIdText.startsWith(TYPE_IRAQI_ID) && cleanedText.length >= 48) {
                val documentNumber = cleanedText.substring(5, 14).replace("<", "")
                val dateOfBirth = cleanedText.substring(29, 36).replace("<", "")
                val gender = cleanedText.substring(36, 37).replace("<", "")
                val expiryDate = cleanedText.substring(38, 44).replace("<", "")

                Log.d(TAG, "رقم الهوية: $documentNumber")
                Log.d(TAG, "تاريخ الميلاد: $dateOfBirth")
                Log.d(TAG, "الجنس: $gender")
                Log.d(TAG, "تاريخ الانتهاء: $expiryDate")

                // بناء كائن MRZ
                val mrzInfo = buildTempMrz(documentNumber, dateOfBirth, expiryDate)
                setMrzInfo(mrzInfo)
                Log.d(TAG, "MRZ Info: ${mrzInfo?.getDocumentNumber()}, ${mrzInfo?.getDateOfBirth()}, ${mrzInfo?.getDateOfExpiry()}")
            }
        } else {
            Log.d(TAG, "لم يتم التعرف على MRZ")
        }
    }

    // ---------------------- بناء MRZ مؤقت ----------------------
    private fun buildTempMrz(documentNumber: String, dateOfBirth: String, expiryDate: String): MRZInfo? {
        return try {
            MRZInfo(
                "P", "IRQ", "", "", documentNumber, "NNN",
                dateOfBirth, Gender.UNSPECIFIED, expiryDate, ""
            )
        } catch (e: Exception) {
            Log.d(TAG, "MRZInfo Error: ${e.localizedMessage}")
            null
        }
    }

    fun cleanMrzText(text: String): String {
        return text.replace(" ", "")
            .replace("\n", "")
            .replace("«", "<")
            .replace(">", "<")
            .replace(Regex("[^A-Z0-9<]"), "") // إزالة أي رموز غير متوقعة
    }


















    private fun setMrzInfo(mrzInfo: MRZInfo?) {

        mrzInfoForView = mrzInfo!!
    }


    override fun onResume() {
        super.onResume()


            val adapter = NfcAdapter.getDefaultAdapter(this)
            if (adapter != null) {
                val intent = Intent(applicationContext, this.javaClass)
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.setAction(NfcAdapter.ACTION_TECH_DISCOVERED);
                val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
                val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
                adapter.enableForegroundDispatch(this, pendingIntent, null, filter)
            }
            if (passportNumberFromIntent) {
                // When the passport number field is populated from the caller, we hide the
                // soft keyboard as otherwise it can obscure the 'Reading data' progress indicator.
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
            }


    }

    override fun onPause() {
        super.onPause()
        val adapter = NfcAdapter.getDefaultAdapter(this)
        adapter?.disableForegroundDispatch(this)
    }




    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (mrzInfoForView==null) {
            Toast.makeText(this, "  برجاء التقاط صورة للوجه الأمامي والخلفي ", Toast.LENGTH_SHORT)
                .show()
        }else{
            if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
                val tag: Tag? = intent.extras?.getParcelable(NfcAdapter.EXTRA_TAG)
                Log.d(TAG, "onNewIntent: " + tag);
                if (tag?.techList?.contains("android.nfc.tech.IsoDep") == true) {
                    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                    val passportNumber = mrzInfoForView!!.documentNumber
                    val expirationDate = mrzInfoForView!!.dateOfExpiry
                    val birthDate = mrzInfoForView!!.dateOfBirth

                    Log.d("PassportData", "Passport Number: $passportNumber, Expiration Date: $expirationDate, Birth Date: $birthDate")

                    if (!passportNumber.isNullOrEmpty() && !expirationDate.isNullOrEmpty() && !birthDate.isNullOrEmpty()) {
                        val bacKey: BACKeySpec = BACKey(passportNumber, birthDate, expirationDate)
                        ReadTask(IsoDep.get(tag), bacKey).execute()
                       // mainLayout.visibility = View.GONE
                        isLoading.value= true
                    } else {

                        Toast.makeText(this, R.string.error_input, Toast.LENGTH_SHORT).show()

                    }
                }
            }
        }

    }

    @SuppressLint("StaticFieldLeak")
    private inner class ReadTask(private val isoDep: IsoDep, private val bacKey: BACKeySpec) : AsyncTask<Void?, Void?, Exception?>() {

        private lateinit var dg1File: DG1File
        private lateinit var dg11File: DG11File
        private lateinit var dg2File: DG2File
        private lateinit var dg14File: DG14File
        private lateinit var sodFile: SODFile
        private var imageBase64: String? = null
        private var bitmap: Bitmap? = null
        private var chipAuthSucceeded = false
        private var passiveAuthSuccess = false
        private lateinit var dg14Encoded: ByteArray

        override fun doInBackground(vararg params: Void?): Exception? {
            try {
                isoDep.timeout = 10000
                val cardService = CardService.getInstance(isoDep)
                cardService.open()
                val service = PassportService(
                    cardService,
                    PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                    PassportService.DEFAULT_MAX_BLOCKSIZE,
                    false,
                    false,
                )
                service.open()
                var paceSucceeded = false
                try {
                    val cardAccessFile = CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
                    val securityInfoCollection = cardAccessFile.securityInfos
                    for (securityInfo: SecurityInfo in securityInfoCollection) {
                        if (securityInfo is PACEInfo) {
                            service.doPACE(
                                bacKey,
                                securityInfo.objectIdentifier,
                                PACEInfo.toParameterSpec(securityInfo.parameterId),
                                null,
                            )
                            paceSucceeded = true
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
                service.sendSelectApplet(paceSucceeded)
                if (!paceSucceeded) {
                    try {
                        service.getInputStream(PassportService.EF_COM).read()
                    } catch (e: Exception) {
                        service.doBAC(bacKey)
                    }
                }
                val dg1In = service.getInputStream(PassportService.EF_DG1)
                dg1File = DG1File(dg1In)

                val dg11In =service.getInputStream(PassportService.EF_DG11)
                dg11File= DG11File(dg11In)

                val dg2In = service.getInputStream(PassportService.EF_DG2)
                dg2File = DG2File(dg2In)
                val sodIn = service.getInputStream(PassportService.EF_SOD)
                sodFile = SODFile(sodIn)

                doChipAuth(service)
                doPassiveAuth()

                val allFaceImageInfo: MutableList<FaceImageInfo> = ArrayList()
                dg2File.faceInfos.forEach {
                    allFaceImageInfo.addAll(it.faceImageInfos)
                }
                if (allFaceImageInfo.isNotEmpty()) {
                    val faceImageInfo = allFaceImageInfo.first()
                    val imageLength = faceImageInfo.imageLength
                    val dataInputStream = DataInputStream(faceImageInfo.imageInputStream)
                    val buffer = ByteArray(imageLength)
                    dataInputStream.readFully(buffer, 0, imageLength)
                    val inputStream: InputStream = ByteArrayInputStream(buffer, 0, imageLength)
                    bitmap = decodeImage(this@MainActivity, faceImageInfo.mimeType, inputStream)
                    imageBase64 = Base64.encodeToString(buffer, Base64.DEFAULT)
                }
            } catch (e: Exception) {
                return e
            }
            return null
        }

        private fun doChipAuth(service: PassportService) {
            try {
                val dg14In = service.getInputStream(PassportService.EF_DG14)
                dg14Encoded = IOUtils.toByteArray(dg14In)
                val dg14InByte = ByteArrayInputStream(dg14Encoded)
                dg14File = DG14File(dg14InByte)
                val dg14FileSecurityInfo = dg14File.securityInfos
                for (securityInfo: SecurityInfo in dg14FileSecurityInfo) {
                    if (securityInfo is ChipAuthenticationPublicKeyInfo) {
                        service.doEACCA(
                            securityInfo.keyId,
                            ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256,
                            securityInfo.objectIdentifier,
                            securityInfo.subjectPublicKey,
                        )
                        chipAuthSucceeded = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }

        private fun doPassiveAuth() {
            try {
                val digest = MessageDigest.getInstance(sodFile.digestAlgorithm)
                val dataHashes = sodFile.dataGroupHashes
                val dg14Hash = if (chipAuthSucceeded) digest.digest(dg14Encoded) else ByteArray(0)
                val dg1Hash = digest.digest(dg1File.encoded)
                val dg2Hash = digest.digest(dg2File.encoded)

                if (Arrays.equals(dg1Hash, dataHashes[1]) && Arrays.equals(dg2Hash, dataHashes[2])
                    && (!chipAuthSucceeded || Arrays.equals(dg14Hash, dataHashes[14]))) {

                    val asn1InputStream = ASN1InputStream(assets.open("masterList"))
                    val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
                    keystore.load(null, null)
                    val cf = CertificateFactory.getInstance("X.509")

                    var p: ASN1Primitive?
                    while (asn1InputStream.readObject().also { p = it } != null) {
                        val asn1 = ASN1Sequence.getInstance(p)
                        if (asn1 == null || asn1.size() == 0) {
                            throw IllegalArgumentException("Null or empty sequence passed.")
                        }
                        if (asn1.size() != 2) {
                            throw IllegalArgumentException("Incorrect sequence size: " + asn1.size())
                        }
                        val certSet = ASN1Set.getInstance(asn1.getObjectAt(1))
                        for (i in 0 until certSet.size()) {
                            val certificate = Certificate.getInstance(certSet.getObjectAt(i))
                            val pemCertificate = certificate.encoded
                            val javaCertificate = cf.generateCertificate(ByteArrayInputStream(pemCertificate))
                            keystore.setCertificateEntry(i.toString(), javaCertificate)
                        }
                    }

                    val docSigningCertificates = sodFile.docSigningCertificates
                    for (docSigningCertificate: X509Certificate in docSigningCertificates) {
                        docSigningCertificate.checkValidity()
                    }

                    val cp = cf.generateCertPath(docSigningCertificates)
                    val pkixParameters = PKIXParameters(keystore)
                    pkixParameters.isRevocationEnabled = false
                    val cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType())
                    cpv.validate(cp, pkixParameters)
                    var sodDigestEncryptionAlgorithm = sodFile.docSigningCertificate.sigAlgName
                    var isSSA = false
                    if ((sodDigestEncryptionAlgorithm == "SSAwithRSA/PSS")) {
                        sodDigestEncryptionAlgorithm = "SHA256withRSA/PSS"
                        isSSA = true
                    }
                    val sign = Signature.getInstance(sodDigestEncryptionAlgorithm)
                    if (isSSA) {
                        sign.setParameter(PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1))
                    }
                    sign.initVerify(sodFile.docSigningCertificate)
                    sign.update(sodFile.eContent)
                    passiveAuthSuccess = sign.verify(sodFile.encryptedDigest)
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }

        override fun onPostExecute(result: Exception?) {
            //mainLayout.visibility = View.VISIBLE
            isLoading.value= false
            if (result == null) {
                val intent = if (callingActivity != null) {
                    Intent()
                } else {
                    Intent(this@MainActivity, ResultActivity::class.java)
                }
                val mrzInfo = dg1File.mrzInfo
                dg11File.nameOfHolder

                intent.putExtra(ResultActivity.KEY_FIRST_NAME, dg11File.nameOfHolder.replace("<", " "))
                intent.putExtra(ResultActivity.KEY_LAST_NAME, mrzInfo.dateOfBirth)
                intent.putExtra(ResultActivity.KEY_GENDER, dg11File.otherNames.toString())
                intent.putExtra(ResultActivity.KEY_STATE,dg11File.nameOfHolder)
                intent.putExtra(ResultActivity.KEY_NATIONALITY, mrzInfo.nationality)
                val passiveAuthStr = if (passiveAuthSuccess) {
                    getString(R.string.pass)
                } else {
                    getString(R.string.failed)
                }
                val chipAuthStr = if (chipAuthSucceeded) {
                    getString(R.string.pass)
                } else {
                    getString(R.string.failed)
                }
                intent.putExtra(ResultActivity.KEY_PASSIVE_AUTH, passiveAuthStr)
                intent.putExtra(ResultActivity.KEY_CHIP_AUTH, chipAuthStr)
                bitmap?.let { bitmap ->
                    if (encodePhotoToBase64) {
                        intent.putExtra(ResultActivity.KEY_PHOTO, imageBase64)
                    } else {
                        val ratio = 320.0 / bitmap.height
                        val targetHeight = (bitmap.height * ratio).toInt()
                        val targetWidth = (bitmap.width * ratio).toInt()
                        intent.putExtra(
                            ResultActivity.KEY_PHOTO,
                            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, false)
                        )
                    }
                }
                if (callingActivity != null) {
                    setResult(AppCompatActivity.RESULT_OK, intent)
                    finish()
                } else {
                    startActivity(intent)
                }
            } else {
               Toast.makeText(this@MainActivity, R.string.error_read, Toast.LENGTH_SHORT).show()
            }
        }
    }















    fun formatDate(input: String): String {
        if (input.length != 6) return "Invalid date"

        val year = input.substring(0, 2).toInt()
        val month = input.substring(2, 4)
        val day = input.substring(4, 6)

        // تحويل السنة إلى 4 أرقام
        val fullYear = if (year < 50) "20$year" else "19$year"

        return "$fullYear-$month-$day"
    }




















    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val KEY_PASSPORT_NUMBER = "passportNumber"
        private const val KEY_EXPIRATION_DATE = "expirationDate"
        private const val KEY_BIRTH_DATE = "birthDate"
    }
}







