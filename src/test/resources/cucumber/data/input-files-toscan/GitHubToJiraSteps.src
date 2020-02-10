Imports System.Data.SqlClient

Private Sub cmdSafe_Click()
Dim user_name As String
Dim password As String
Dim query As String
Dim rs As DAO.Recordset

    ' Get the user name and password.
    user_name = Replace$(txtUserName.Text, "'", "''")
    password = Replace$(txtPassword.Text, "'", "''")

    ' Compose the query.
    query = "SELECT COUNT (*) FROM Passwords " & _
        "WHERE UserName='" & user_name & "'" & _
        "  AND Password='" & password & "'"
    txtQuery.Text = query

    ' Execute the query.
    On Error Resume Next
    Set rs = m_DB.OpenRecordset(query, dbOpenSnapshot)
    If Err.Number <> 0 Then
        lblValid.Caption = "Invalid Query"
    ElseIf (CInt(rs.Fields(0)) > 0) Then
        lblValid.Caption = "Valid"
    Else
        lblValid.Caption = "Invalid"
    End If

    rs.Close
End Sub


Private Sub cmdUnsafe_Click()
Dim user_name As String
Dim password As String
Dim query As String
Dim rs As DAO.Recordset

    ' Get the user name and password.
    user_name = txtUserName.Text
    password = txtPassword.Text

    ' Compose the query.
    query = "SELECT COUNT (*) FROM Passwords " & _
        "WHERE UserName='" & user_name & "'" & _
        "  AND Password='" & password & "'"
    txtQuery.Text = query

    ' Execute the query.
    On Error Resume Next
    Set rs = m_DB.OpenRecordset(query, dbOpenSnapshot)
    If Err.Number <> 0 Then
        lblValid.Caption = "Invalid Query"
    ElseIf (CInt(rs.Fields(0)) > 0) Then
        lblValid.Caption = "Valid"
    Else
        lblValid.Caption = "Invalid"
    End If

    rs.Close
End Sub


p = txtP.Text
Dim conn As New ADODB.Connection
conn.Open "connection string"

Dim cmd As New ADODB.Command
With cmd
    .ActiveConnection = conConnection
    .CommandText = "SELECT fields FROM table WHERE condition = ?"
    .CommandType = adCmdText
End With

Dim param As New ADODB.Parameter
Set param = cmd.CreateParameter("condition", adVarChar, adParamInput, 5, "value")
cmd.Parameters.Append p

Dim rs As New ADODB.Recordset
rs.CursorLocation = adUseClient
rs.Open cmd, , adOpenStatic, adLockOptimistic

Dim temp
Do While Not rs.EOF
    temp = rs("field")
    rs.MoveNext
Loop

rs.Close
conn.Close



Public Class Form1
    Private Sub Button1_Click(ByVal sender As System.Object, _
                        ByVal e As System.EventArgs) Handles Button1.Click
        Dim con As SqlConnection = New SqlConnection( _
                        "Data Source=.;Integrated Security=True;AttachDbFilename=D:\myDB.mdf")
        con.Open()
        Dim cmdText As String = _
                        "INSERT INTO Customer(UserName, [Password]) VALUES (@UserName,@Password)"
        Dim cmd As SqlCommand = New SqlCommand(cmdText, con)
        With cmd.Parameters
            .Add(New SqlParameter("@UserName", txtUserName.Text))
            .Add(New SqlParameter("@Password", txtPassword.Text))
        End With
        cmd.ExecuteNonQuery()
        con.Close()
        con = Nothing
    End Sub
End Class