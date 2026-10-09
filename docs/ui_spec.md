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
| money_request | Amber | Possible scam detected | Tawagan muna ang tao at siguraduhing siya talaga ito bago magpadala ng pera. | Call the person first and make sure it is really them before you send money. |
| ai_scam | Amber | Possible scam detected | Huwag magpadala ng pera, OTP o personal na impormasyon. | Do not send money, OTP, or personal information. |

warnings.json format becomes: `{ "<type>": { "severity": "red" | "amber", "title": "...", "message": { "fil": "...", "en": "..." } } }`

## Main screen

- Header: "Scam Guardian" (unchanged).
- The big on/off button (unchanged).
- Status line under the button:
  - ON and service enabled: "You are secured" (green).
  - Otherwise: "You are not secured" (red), plus a button "Turn on protection" that opens the setup steps / Accessibility settings.
- Language toggle "EN | FIL".
- Privacy line at the bottom, English only, with a shield icon: "Uses local engine only. Not connected to the internet."
- Small text links: "Test a message" and "History".

## History screen

- A list of past warnings, newest first. Stored locally on the phone (a JSON file in app storage is enough). Keep the last 200 entries.
- Each entry shows:
  - Icon in the warning color (red: alert icon; amber: caution icon).
  - Title ("Scam detected" / "Possible scam detected").
  - Message in the selected language (with the brand name filled in).
  - App name where it was seen (for example Messenger) and timestamp (for example "Oct 9, 10:24 PM").
- If the language setting changes, existing entries show the new language (store the type and brand, not the final text).
- Empty state: "No warnings yet."
- A "Clear history" text button at the bottom.

## Warning banner

- Banner at the top of the screen, slides down. Not a blocking dialog.
- Card color by severity (red or amber), white text, warning icon.
- Title, message in the selected language, brand name in bold.
- A small "Why?" link that expands the evidence line (for example "BDO mentioned; link goes to bdo-bd0.cc").
- Buttons: "Close" and "Not a scam".
- Short vibration when it appears. Auto-hide after 15 seconds.
- Every banner also creates a history entry.
