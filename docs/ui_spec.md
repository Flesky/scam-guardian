# Scam Guardian: UI/UX spec (decided)

Give this file to the agent together with the prompts for the main screen, history, and banner steps.

## General

- Flat style. Compose foundation primitives, no Material components.
- Icons: vector drawables in res/drawable (for example imported from Material Symbols as XML). No icon library dependency.
- Large text for older users: banner and history text at least 18sp, buttons at least 48dp, high contrast.
- Two colors for warnings:
  - Red: Scam detected (fake_link, otp_request).
  - Amber: Possible scam detected (risky_link, money_request, ai_scam).

## Language setting

- Two options: English and Filipino (Taglish). Default: Filipino.
- The setting changes ONLY the warning messages (banner) and the history entries. All other app text stays in English.
- Store the choice locally (SharedPreferences or DataStore).
- Place a small two-option toggle "EN | FIL" on the main screen, below the status line.

## Warning text

| Type | Color | Title (both languages) | Filipino message | English message |
| --- | --- | --- | --- | --- |
| fake_link | Red | Scam detected | Hindi ito ang tunay na link ng {brand}. Huwag magbigay ng OTP o personal na impormasyon. | Not a real link of {brand}. Do not give your OTP or personal info. |
| otp_request | Red | Scam detected | Huwag ibigay ang OTP. Hindi kailanman manghihingi ng OTP ang mga bangko o kumpanya. | Do not give your OTP. Banks and companies will never ask for your OTP. |
| risky_link | Amber | Possible scam detected | Mag-ingat. Kahina-hinala ang link na ito. | Be careful. This link looks suspicious. |
| scam_claim | Amber | Possible scam detected | Mag-ingat. Ang nagsasabing "hindi ito scam" ay madalas na scam. | Be careful. Messages that say "this is not a scam" are often scams. |
| refund_request | Amber | Possible scam detected | Wag i-entertain ang mga nagpapabalik ng pera. Hayaan silang mag-reach out sa customer service para maibalik ang pera nila. | Do not entertain people who ask you to send money back. Let them contact customer service to get their money back. |
| money_request | Amber | Possible scam detected | Tawagan muna ang tao at siguraduhing siya talaga ito bago magpadala ng pera. | Call the person first and make sure it is really them before you send money. |
| ai_scam | Amber | Possible scam detected | Huwag magpadala ng pera, OTP o personal na impormasyon. | Do not send money, OTP, or personal information. |

warnings.json format becomes: `{ "<type>": { "severity": "red" | "amber", "title": "...", "message": { "fil": "...", "en": "..." } } }`

## Main screen

The app has one screen. Everything under the header scrolls as one list.

- Header: "Scam Guardian", with a gear icon at the top right. It opens the platform's popup menu with two items:
  - "Demo mode", with a check box. In demo mode every warning shows its banner at once, with no 30-second wait between banners in the same app.
  - "Switch to Filipino" or "Switch to English", whichever is not the current language of the warnings.
- The big on/off button. It shows ON only when the user is really protected: the button is on and the accessibility service is enabled. Tapping it while off turns protection on and, if the service is not enabled, opens the Accessibility settings. There is no other button for this and no instruction text.
- Status under the button: "You are secured" (green) or "You are not secured" (red).
- Privacy line, English only, with a shield icon: "Uses local engine only. Not connected to the internet."
- The button, status and privacy line fill the screen. History starts below them, under a "History" title, so it is only seen after scrolling (see below).
- At the bottom of the first screen: "Scroll up for history" under a double-chevron icon that gently moves up and down. Tapping it scrolls to the history.

## History

- Part of the main screen's list, not a separate screen.
- Past warnings, newest first. Stored locally on the phone (a JSON file in app storage is enough). Keep the last 200 entries.
- Each entry shows:
  - Icon in the warning color (red: alert icon; amber: caution icon).
  - Title ("Scam detected" / "Possible scam detected").
  - Message in the selected language (with the brand name filled in).
  - The message that caused the warning, in quotes, up to four lines.
  - App name where it was seen (for example Messenger) and timestamp (for example "Oct 9, 10:24 PM").
- If the language setting changes, existing entries show the new language (store the type and brand, not the final text).
- Empty state: "No warnings yet."
- A message that already has a history entry does not warn again. Clearing the history resets this.
- With entries, a "Clear history" button is the last item of the list.

## Warning banner

- Banner at the top of the screen, slides down. Not a blocking dialog.
- Card color by severity (red or amber), white text, warning icon.
- Title, message in the selected language, brand name in bold.
- A small "Why?" link that expands the evidence line (for example "BDO mentioned; link goes to bdo-bd0.cc").
- Buttons: "Close" and "Not a scam".
- Short vibration when it appears. Auto-hide after 15 seconds.
- Every banner also creates a history entry.
