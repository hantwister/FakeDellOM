# FakeDellOM
A fake host that can be "managed" by Dell OMSA, getting you past the login screen, and
possibly serving as a starting point for fuzzing or other testing.

## Backstory
While researching potential vulnerabilities¹ in Dell's OpenManage Server Administrator,
I was interested both in:

1. not having to lug around a Dell server that I had admin creds to, so I could easily
   take advantage of vulnerabilities that just required getting past the login screen¹,
   and
   
2. seeing if new vulnerabilities could result from a "fake" server (managed node) that
   I controlled, which would respond in unexpected and malicious ways to requests by OMSA.

¹Such as: https://www.exploit-db.com/exploits/39486/

## What this is
A little bit of _really quick and dirty_ Java code, intended to listen for requests by
OMSA, and respond as you desire.  At the time of writing, it only has a response for
two requests made during the login process, which was enough to get you into² an OMSA
instance as a "true"³ admin.  If you want to target other kinds of requests, poke
around the application, see what POST data OMSA sends, get an idea of what is expected
back, form a response of your choosing, and build that logic into this code.

²Right after a successful login, OMSA will redirect you to a start page that will
 kick off a deluge of other requests. You'll want to stop your browser, as OMSA has
 a tendency to redirect you back to the login screen when it encounters errors, and
 it will if this code hasn't been modified to deal with those subsequent requests,
 and doesn't respond in an appropriate manner.

³There are apparently two separate sets of rights, and depending on what code you're
 mucking with, which set it uses may vary.  There seem to be only three bits used per
 set, so 7 (4 + 2 + 1) represents the maximum rights in each set.  One set occupies
 the upper 16 bits of a 32-bit integer, while the second set occupies the lower.
 Ultimately, the fake managed node responds with `7 + (7 << 16)`.  If your intent is
 to test if OMSA restricts access to certain resources based on user rights, you might
 choose to change this value.

## What this is NOT
This is not at all intended to help you manage your Dell servers, nor any other server.
While you could try to reimplement all of Dell's secret sauce, that is far removed from
the intent of this code.  If, unlike this code, you intend to faithfully reproduce Dell
behaviors in response to Dell-designed queries, you may want to consult a lawyer, as
Oracle v Google ( https://www.eff.org/cases/oracle-v-google ) or other legal issues could
apply.

## How to use it
The end result is wanting to have the `fakedell.XMLResponder` servlet accessible at
`https://[yourIP]:5986/wsman`

The server should be using a self-signed certificate, and when logging into OMSA,
you'll provide your IP as the remote host to manage, and choose to ignore certificate
warnings.

The `web.xml` here assumes it is the root application, and maps the servlet to `/wsman`.
You may need to adjust this to work with your configuration.

You may choose to have the Java server, or some other tool, log all in/out traffic. For
example, I had Tomcat listen on port 8080, had stunnel listen on port 5986, and used
Wireshark to capture all port 8080 traffic on the loopback device.

Have fun~

### Obligatory legalese

If you have a strong case for an alternate license, drop me a line and we can talk.
(Though, honestly, this being a dinky little PoC and skeleton frame more than anything
else, you could probably recreate it fairly easily.)

Otherwise,

    FakeDellOM
    Copyright (C) 2016 Harrison Neal, hneal@imalazybastard.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
