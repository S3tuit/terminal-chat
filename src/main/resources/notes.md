- mongo index to speed up queries that find the *n* most recent messages of a certain Chat:
~~~~sql
db.Message.createIndex({ chatId: 1, timestamp: -1 })
~~~~

