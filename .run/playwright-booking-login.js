async (page) => {
  await page.goto('http://127.0.0.1:5174/')
  await page.evaluate(({ accessToken, refreshToken }) => {
    localStorage.setItem('fitness_app_token', JSON.stringify({
      accessToken,
      refreshToken,
      user: null
    }))
  }, {
    accessToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEwMSwidXNlcm5hbWUiOiJyb290X21lbWJlciIsInR5cGUiOiJhY2Nlc3NfdG9rZW4iLCJwbGF0Zm9ybSI6ImFwcCIsInRva2VuVmVyc2lvbiI6MCwic3ViIjoicm9vdF9tZW1iZXIiLCJpYXQiOjE3NzcwODcwNDksImV4cCI6MTc3NzA5NDI0OX0.qhTwrBWuTKfeea5fxGEsziaMvPzcV6N9096sqoUZNTU',
    refreshToken: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEwMSwidXNlcm5hbWUiOiJyb290X21lbWJlciIsInR5cGUiOiJyZWZyZXNoX3Rva2VuIiwicGxhdGZvcm0iOiJhcHAiLCJ0b2tlblZlcnNpb24iOjAsInN1YiI6InJvb3RfbWVtYmVyIiwiaWF0IjoxNzc3MDg3MDQ5LCJleHAiOjE3NzgzODMwNDl9.HId-lM95ezNXdmQYUz1PF5mVtPnX_Ce1THHY8bbFDlk'
  })
  await page.goto('http://127.0.0.1:5174/booking?bookingTab=bookings')
}
