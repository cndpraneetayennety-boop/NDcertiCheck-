package com.example.util

import com.example.model.CertificateAudit
import com.example.model.LetterTemplateType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LetterGenerator {

    fun generateLetter(
        audit: CertificateAudit,
        templateType: LetterTemplateType,
        recipientAuthority: String = "The Controller of Examinations / Registrar",
        institutionName: String = "",
        studentContact: String = "",
        studentAddress: String = ""
    ): String {
        val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val effectiveInstitution = institutionName.ifBlank {
            audit.discrepancies.firstOrNull { it.field.name == "INSTITUTION" }?.officialValue
                ?: "The Academic University / Institute"
        }
        val rollNo = audit.discrepancies.firstOrNull { it.field.name == "ROLL_NUMBER" }?.officialValue
            ?: "N/A"
        val degree = audit.discrepancies.firstOrNull { it.field.name == "DEGREE_TITLE" }?.officialValue
            ?: "Degree / Diploma"

        val discrepanciesOnly = audit.discrepancies.filter { !it.isMatch }

        val discrepancyTable = StringBuilder()
        discrepancyTable.append("+-----+-------------------------+-------------------------+-------------------------+\n")
        discrepancyTable.append("| S.N | Parameter / Field       | Printed on Certificate  | Official Record (Correct)|\n")
        discrepancyTable.append("+-----+-------------------------+-------------------------+-------------------------+\n")
        if (discrepanciesOnly.isEmpty()) {
            discrepancyTable.append("|  -  | (No discrepancy noted)  | (All records match)     | (100% Verified Match)   |\n")
        } else {
            discrepanciesOnly.forEachIndexed { index, disc ->
                val fieldName = disc.field.displayName.take(23).padEnd(23)
                val certVal = disc.certificateValue.take(23).padEnd(23)
                val offVal = disc.officialValue.take(23).padEnd(23)
                discrepancyTable.append("|  ${index + 1}  | $fieldName | $certVal | $offVal |\n")
            }
        }
        discrepancyTable.append("+-----+-------------------------+-------------------------+-------------------------+\n")

        return when (templateType) {
            LetterTemplateType.ACADEMIC_CORRECTION -> buildStandardAcademicLetter(
                currentDate = currentDate,
                recipientAuthority = recipientAuthority,
                institution = effectiveInstitution,
                studentName = audit.officialName,
                rollNo = rollNo,
                degree = degree,
                discrepancyTable = discrepancyTable.toString(),
                studentContact = studentContact,
                studentAddress = studentAddress
            )
            LetterTemplateType.URGENT_EMPLOYMENT_VISA -> buildUrgentVisaLetter(
                currentDate = currentDate,
                recipientAuthority = recipientAuthority,
                institution = effectiveInstitution,
                studentName = audit.officialName,
                rollNo = rollNo,
                degree = degree,
                discrepancyTable = discrepancyTable.toString(),
                studentContact = studentContact
            )
            LetterTemplateType.MARKSHEET_ERRATA -> buildMarksheetErrataLetter(
                currentDate = currentDate,
                recipientAuthority = recipientAuthority,
                institution = effectiveInstitution,
                studentName = audit.officialName,
                rollNo = rollNo,
                degree = degree,
                discrepancyTable = discrepancyTable.toString()
            )
            LetterTemplateType.DUAL_NAME_AFFIDAVIT -> buildNameDiscrepancyAffidavit(
                currentDate = currentDate,
                institution = effectiveInstitution,
                studentName = audit.officialName,
                printedName = audit.candidateName,
                rollNo = rollNo,
                degree = degree,
                discrepancyTable = discrepancyTable.toString()
            )
        }
    }

    private fun buildStandardAcademicLetter(
        currentDate: String,
        recipientAuthority: String,
        institution: String,
        studentName: String,
        rollNo: String,
        degree: String,
        discrepancyTable: String,
        studentContact: String,
        studentAddress: String
    ): String {
        return """
Date: $currentDate

To,
$recipientAuthority
$institution
Academic Examination & Certificate Cell

Subject: Formal Application for Rectification of Typographical / Data Discrepancy in Academic Certificate

Respected Sir / Madam,

I, $studentName, was a bonafide student of $institution, enrolled in the $degree programme under Registration / Roll Number: $rollNo.

Upon recent audit and verification of my original academic certificate against my institutional records and primary identity documents (Matriculation Certificate / National ID), the following typographical clerical discrepancies were detected:

$discrepancyTable

As evident from the tabulated audit, the discrepancies appear to be inadvertent typographical or clerical transcription errors occurred during the typesetting/issuance of the original document. These mismatches are causing substantial hindrance in my ongoing academic verifications and background credential clearance.

Therefore, I earnestly request your esteemed office to verify these details against the University Master Gazette / Ledger and issue a rectified certificate or an official errata endorsement at your earliest convenience.

Enclosures for Verification:
1. Copy of the issued Certificate with highlighted discrepancies.
2. Attested copy of Official Registry / Matriculation Certificate establishing authentic spelling.
3. Copy of University Identity Card and Hall Ticket.
4. Copy of Government-issued Identity Card (Passport / Aadhaar / National ID).

Thanking you.

Yours faithfully,

_______________________
($studentName)
Roll No: $rollNo
Program: $degree
${if (studentContact.isNotBlank()) "Contact: $studentContact\n" else ""}${if (studentAddress.isNotBlank()) "Address: $studentAddress\n" else ""}
""".trimIndent()
    }

    private fun buildUrgentVisaLetter(
        currentDate: String,
        recipientAuthority: String,
        institution: String,
        studentName: String,
        rollNo: String,
        degree: String,
        discrepancyTable: String,
        studentContact: String
    ): String {
        return """
Date: $currentDate
URGENT - EXPEDITED VERIFICATION & BACKGROUND CLEARANCE

To,
$recipientAuthority
$institution

Subject: Urgent Request for Certificate Data Discrepancy Clarification Letter / NOC for Visa & Higher Education Verification

Respected Authority,

I am writing to bring to your urgent attention a clerical discrepancy on my academic credentials ($degree, Roll No: $rollNo) for student $studentName. 

My documents are currently under strict evaluation by foreign credential evaluation agencies (WES / Embassies / Employment Verification). An automated audit identified the following discrepancy between my certificate and institutional records:

$discrepancyTable

Due to imminent visa/admission deadlines, I respectfully request an interim Official Clarification Letter / No Objection Certificate (NOC) affirming my verified identity and confirming that the typographical error is recognized and undergoing internal rectification.

I stand ready to deposit any prescribed emergency fees and present original credentials for spot physical inspection.

Sincerely,

_______________________
$studentName
Candidate Roll: $rollNo
${if (studentContact.isNotBlank()) "Emergency Contact: $studentContact\n" else ""}
""".trimIndent()
    }

    private fun buildMarksheetErrataLetter(
        currentDate: String,
        recipientAuthority: String,
        institution: String,
        studentName: String,
        rollNo: String,
        degree: String,
        discrepancyTable: String
    ): String {
        return """
Date: $currentDate

To,
$recipientAuthority
$institution
Section: Tabulation & Grade Evaluation

Subject: Petition for Rectification of Grade / Marks / Ledger Calculation Discrepancy

Respected Sir / Madam,

With reference to the academic results and certificate issued for $degree (Roll No: $rollNo) in favor of $studentName, I respectfully submit this petition regarding calculation/entry errors:

$discrepancyTable

Kindly arrange for a re-tabulation against the official award lists and examination master ledgers, and issue the corrected grade transcript accordingly.

Enclosed:
1. Certified copies of semester-wise grade sheets.
2. Original transcript showing discrepancies.

Respectfully submitted,
_______________________
$studentName
Registration No: $rollNo
""".trimIndent()
    }

    private fun buildNameDiscrepancyAffidavit(
        currentDate: String,
        institution: String,
        studentName: String,
        printedName: String,
        rollNo: String,
        degree: String,
        discrepancyTable: String
    ): String {
        return """
DECLARATION & AFFIDAVIT OF IDENTITY / ONE AND THE SAME PERSON

Date: $currentDate
Place: Examination Jurisdiction

I, $studentName, son/daughter of the guardian recorded in official archives, residing at the registered address, do solemnly affirm and declare as under:

1. That I am the identical person who completed the program $degree from $institution under Roll / Registration No: $rollNo.

2. That on the certificate issued by the institution, my name has been printed as "$printedName", whereas my authentic legal name as per my Birth/High School Certificate is "$studentName".

3. Summary of Discrepancy Identified:
$discrepancyTable

4. That both names "$printedName" and "$studentName" pertain to one and the same person, viz. myself, and the discrepancy is purely clerical in nature without any fraudulent intent.

5. I request the concerned administrative authorities and verification officers to accept this declaration along with supporting government identity documents.

DEPONENT:

_______________________
($studentName)
Roll No: $rollNo
Institution: $institution
""".trimIndent()
    }
}
