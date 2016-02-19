<%
Dim nPin, sPin, str

if (Request("global")<>"") Then
  nPin = Request("global")
  str = "global"
  Do While (Application("pin" & nPin)<>"")
    str = str & "&pin" & nPin & ":" & Application("pin" & nPin)
    nPin = nPin + 1
  Loop
  for n=0 to 1000000
    a = a + n
  next 
  Response.Write str
Else
  if Request("tem")<>"" Then
    nPin = Request("tem")
    sPin = "tem" & nPin
    Application(sPin) = Application(sPin) + 1
  Else
    nPin = Request("pin")
    
    sPin = "pin" & nPin
    if (Application(sPin)="ON") Then
      Application(sPin) = "OFF"
    else
      Application(sPin) = "ON"
    End If
  End IF
  for n=0 to 1000000
    a = a + n
  next 
  response.write sPin & ":" & Application(sPin) 
End If
%>

