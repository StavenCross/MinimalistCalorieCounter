## User Guide

#### Quality of Life

- By default, there are 19 Easy Access buttons on the home screen to log Food with a single click. You can long-press a button to go directly to this Food's edit page where you can disable its Easy Access switch if so desired. To add new buttons, click a Food in the Database and turn on the switch.
- On any Food's edit page, you can add set Custom Quickselect Weights (e.g., "55:1 Scoop-110:2 Scoops"). These appear below the Standard Quickselect Weights, taking up two columns.
- You can repeatedly press the "Day" button in the navigation bar to toggle between the "add" and "edit" pages.
- In the "Add Food" page, the "Enter" key on the keyboard is replaced by a "Next" key to jump to the next text field, except on the final text field where it becomes a "Checkmark" key to instantly save instead.
- The default Database includes an "Average" Food that is set to exactly 100kcal per 100g while maintaining a healthy balance of nutrients. If I estimate based on experience that a meal was roughly 700kcal, I simply log 700g of "Average" and call it good enough.

#### Health Connect

- Add Meal can write one Health Connect Nutrition record per parsed food, including calories, macros, fiber, meal type, and timestamp.
- The Meals screen reads app-owned Health Connect Nutrition records for the selected day so logged meals can be reviewed and deleted.
- Goals can read authorized Health Connect body metrics, such as weight, height, body fat, and lean mass, to fill unlocked goal fields.
- Settings includes date-range tools for deleting app-owned Health Connect Nutrition records and exporting authorized Health Connect data to CSV.
- Health Connect has strict write-rate limits. Historical meal imports are chunked and retried when quota errors occur.
- Health Connect is by default local-only, but to protect against data loss, if you search for "Health Connect" in your phone settings, then click "Manage Data" and "Backup and restore", you can set a daily, weekly or monthly upload to Google Drive.

#### Tinkering

- If desired, it is possible to set your Database in a different language than the app UI. Set the app to your desired Database language, reset the Database, and then switch the app back to your preferred UI language.
- There is no option in the app for that, but if you want to completely clear out the Database, you can import [database_empty.csv](https://github.com/Makstuff/MinimalistCalorieCounter/blob/9da0ef3786e6f7f54b71d335681c2f8e5c225166/database_empty.csv).
- In case you end up manually editing the database CSV file, ensure it follows the [default format](https://github.com/Makstuff/MinimalistCalorieCounter/tree/3cd6049b0cee640e6fdbafbaff69f8ea3e6b145c/app/src/main/res/raw) and especially avoid illegal symbols like the classic culprits ";|,. Due to my lack of motivation as well as coding skill, the error messages of CSV import function are not particularly useful, so finding small errors like a comma in "3,5% whole milk" can be quite tricky.

#### Missing features

This section is mainly based on emails I have gotten that request various features. I am very open to small tweaks and fixed and QoL stuff, but given the crappy structure of my base code, most major changes and extensions are super annoying to do without breaking anything, and need a lot of motivation. Given that I personally am mostly happy with the app's current set of features, this motivation can be hard to find.

- Micronutrients (like Vitamins and Minerals) are a similar case. Given how many there are and that most people care either about none at all or a very limited but varying subset of them, essentially the user would have to be able to pick what they want to track. Which would require a custom database structure for each user. Again, not impossible, but annoying to add and rather niche so probably never.

- Historical database entry screens still inherit the original app's European-style label assumptions. The Add Meal workflow is the preferred path for US-style nutrition blurbs.

## About this app

After testing many calorie counter apps without finding a perfect fit, I finally decided to take matters into my own hand and learn how to code for Android myself. This app is the final result of my efforts.

It is optimized for a minimum number of clicks in the long-term, for which it sacrifices significant intuitivity and ease of use in the short-term. First and foremost, it does not use a large external Database, as I prefer to verify all nutritional data directly from product labels or trusted sources like my nutrition table book. The interface is also structured in a way that makes for a steeper learning curve than most comparable apps, but once you get the hang of it and have customized Easy Access Foods and Quickselect Weight buttons to your liking, the speed of logging becomes incredible.
 
The early versions of this app until about version code 3 were coded 100% manually. For more complicated stuff I added later, like Health Connect support and the French, Italian, and Spanish translations, I however did massively rely on Gemini. Since for my skill level these things were pretty much impossible to do on my own, I am just glad I could get them done at all. By the way, if you find nonsensical translations in your language, please let me know.


## Screenshots

 <img width="3200" height="1563" alt="rect1" src="https://github.com/user-attachments/assets/ad0e5925-3520-4045-8206-b74a737bfdff" />

<a href='https://play.google.com/store/apps/details?id=com.makstuff.minimalistcaloriecounter&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' width="200"/></a>

My email: <message.makstuff@gmail.com>

Donations: [Buy me a coffee.](https://www.buymeacoffee.com/makstuff)
