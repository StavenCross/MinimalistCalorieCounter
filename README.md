## User Guide

- **Easy access buttons**: By default, there are 19 buttons on the home screen to log foods with a single click. Long-press a button to go directly to this food's edit screen where you can disable its "Easy Access" switch if so desired. To add new buttons, click a food in the database and turn on the switch.
- **Custom quickselect weights**: On any food's edit page, you can add set weights (e.g., "55:1 Scoop-110:2 Scoops"). These appear as one-click buttons below the standard quickselect weights, taking up two columns.
- **Navigation shortcut**: Repeatedly press the "Day" button in the navigation bar to toggle between the "add" and "edit" pages even faster.
- **Adaptive "Enter" key**: In multi-field input screens, the "Enter" key is replaced by a "Next" key to jump between fields, except on the final field where it becomes a "Checkmark" key to quickly confirm and save.
- **Recipe weight autocomplete**: When creating a recipe, leave the "Overall weight" field empty to automatically use the calculated sum of all ingredient weights. This can be useful for many dishes where no extra water is added.
- **Logging unknown food**: The default database includes an "Average" food that is set to exactly 100kcal per 100g while maintaining a healthy balance of nutrients. If I estimate based on experience that a meal was roughly 700kcal, I simply log 700g of "Average" and call it good enough.
- **Multiple languages**: You can maintain your database in a different language than the app UI. Set the app to your desired database language, reset the database, and then switch the app back to your preferred UI language.
- **Health Connect behavior**: If auto sync is active, every added, changed or deleted single archive entry will be instantly mirrored in Health Connect. Bulk changes (Import Archive, Clear Archive) do not automatically trigger syncs and must be triggered manually if desired (via "Export Archive (Health Connect)"). To be able to monitor what's happening in Health Connect, every write action is accompanied by a toast notification (e.g., "Archive entry added to Health Connect").
- **Clear database**: There is no option in the app for that, but if desired you can import [database_empty.csv](https://github.com/Makstuff/MinimalistCalorieCounter/blob/9da0ef3786e6f7f54b71d335681c2f8e5c225166/database_empty.csv) to completely clear out the database.
- **CSV format**: When tinkering with CSV files, ensure they follow the [default format](https://github.com/Makstuff/MinimalistCalorieCounter/tree/3cd6049b0cee640e6fdbafbaff69f8ea3e6b145c/app/src/main/res/raw) and especially avoid illegal symbols like the classic culprits ";|,. The error messages of CSV import are not particularly useful, so finding small errors like a comma in "3,5% whole milk" can be quite tricky.

## About this app

After testing many calorie counter apps without finding a perfect fit, I finally decided to take matters into my own hand and learn how to code for Android myself. This app is the final result of my efforts.

It is optimized for a minimum number of clicks in the long-term, for which it sacrifices significant intuitivity and ease of use in the short-term. First and foremost, it does not use a large external database, as I prefer to verify all nutritional data directly from product labels or trusted sources like my nutrition table book. The interface is also structured in a way that makes for a steeper learning curve than most comparable apps. I might be biased of course, but I say, totally worth it.

## Production details
 
The early versions of this app were 100% coded manually. I did however massively rely on Gemini for polishing the UI, adding Health Connect support as well as the French, Italian, and Spanish translations. All these were pretty much impossible to do for me on my own. By the way, if you find nonsensical translations in your language, please let me know at message.makstuff@outlook.com.

Another detail is that during the initial development I was not aware of how weird USA and Canada's food labels are with their arbitrary "portions" and including fiber in carbohydrate. So the app is incompatible with this type of labels, and fixing this would be a major headache even with Gemini, so I decided to just exclude USA and Canada from the target regions in the Play Store. If your country should be excluded as well, please write me at message.makstuff@outlook.com.

## Screenshots

 ![Combo](https://github.com/user-attachments/assets/fe823df8-814a-4ebe-8026-e3526f95ffc5)

<a href='https://play.google.com/store/apps/details?id=com.makstuff.minimalistcaloriecounter&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="200"/></a>

[Buy me a coffee.](https://www.buymeacoffee.com/makstuff)