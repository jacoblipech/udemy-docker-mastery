def getReviewContent(
      hotelId: Iterator[String],
      config: YanoljaConfig,
      httpClient: HttpClient
  ): Iterator[Future[YanoljaReviewContentResponse]] = {
    hotelId.map { hid =>
      {
        val request = getReviewContentRequest(hid, config)
        httpClient.send(request).flatMap { response =>
          if (response.code == 200) {
//            val rawBody: String = """{"code":200,"data":{"total":17,"reviews":[{"propertyId":"1000091757","roomTypeId":"118029","reviewId":"442072","language":"ko","reviewer":"껄*******","title":null,"content":"`123123123123123","rating":4.0,"ratings":{"services":1.0,"clean":1.0,"convenience":1.0,"location":1.0},"createdAt":"2019-09-24T15:53:50"},{"propertyId":"1000091757","roomTypeId":"118007","reviewId":"442067","language":"ko","reviewer":"ㅎ*******","title":null,"content":"rrrrrgvvhh","rating":5.0,"ratings":{"services":5.0,"clean":5.0,"convenience":5.0,"location":5.0},"createdAt":"2019-08-14T17:33:52"},{"propertyId":"1000091757","roomTypeId":"118007","reviewId":"442066","language":"ko","reviewer":"닉*******","title":null,"content":"후기 수정 테스트~<img src=\"https://yadev.yanolja.com/v5/2019/08/06/16/640/5d492b8def1899.73822394.jpeg\"><br /><img src=\"https://yadev.yanolja.com/v5/2019/08/05/14/640/5d47c0e8a93a17.06643331.jpg\"><br />","rating":3.0,"ratings":{"services":5.0,"clean":2.0,"convenience":5.0,"location":4.0},"createdAt":"2019-08-06T16:26:07"},{"propertyId":"1000091757","roomTypeId":"118007","reviewId":"442065","language":"ko","reviewer":"닉*******","title":null,"content":"fdasfds<img src='https://yadev.yanolja.com/v5/2019/08/05/18/640/5d47fa86150d78.57394863.png' alt='4049956'><br />","rating":1.0,"ratings":{"services":2.0,"clean":3.0,"convenience":2.0,"location":3.0},"createdAt":"2019-08-05T18:44:39"},{"propertyId":"1000091757","roomTypeId":"118007","reviewId":"442063","language":"ko","reviewer":"닉*******","title":null,"content":"gfadsfadasdf","rating":2.0,"ratings":{"services":2.0,"clean":2.0,"convenience":4.0,"location":2.0},"createdAt":"2019-08-05T15:58:56"},{"propertyId":"1000091757","roomTypeId":"118007","reviewId":"442060","language":"ko","reviewer":"닉*******","title":null,"content":"또 오곳 ㅣㅍㄴㅇ마ㅣㄹㄴㅇ\n하하ㅏ\n고양이 귀여움<img src=\"https://yadev.yanolja.com/v5/2019/08/01/18/640/5d42abeec95507.10978663.jpg\"><br /><img src=\"https://yadev.yanolja.com/v5/2019/08/01/18/640/5d42abef100af8.75377962.jpg\"><br /><img src=\"https://yadev.yanolja.com/v5/2019/08/01/18/640/5d42abef6ea847.73644889.PNG\"><br />","rating":3.0,"ratings":{"services":3.0,"clean":5.0,"convenience":3.0,"location":1.0},"createdAt":"2019-08-01T18:07:59"},{"propertyId":"1000091757","roomTypeId":"118022","reviewId":"442054","language":"ko","reviewer":"안*******","title":null,"content":"하하하하 좋아요 하하하하 호호호\n후루루\nㅜ루루루루하하라라\nㅁㄴ아ㅣ럼ㄴ;ㅣㅏㅇ러\n호호홍ㅎㅎㅎ\n버벅거려 ㅠㅠ\n버벅거리지 말<img src=\"https://yadev.yanolja.com/v5/2019/07/29/20/640/5d3edb3b450dd7.88581283.jpg\"><br />","rating":1.0,"ratings":{"services":2.0,"clean":5.0,"convenience":2.0,"location":1.0},"createdAt":"2019-07-29T20:40:44"}]}}"""
            val rawBody: String = response.unsafeBody
             println(s"raw body 1 is $rawBody")
            decode[YanoljaReviewContentResponse](rawBody) match {
              case Right(result) =>
                processYanoljaReview(result, config, httpClient)
              case Left(e) =>
                logger.error(s"error when parsing raw Yanolja hotel review content response $rawBody", e)
                throw e
            }
          } else {
            logger.error(s"Failed to obtain Yanolja review: ${response.statusText} ${response.code}")
            throw new Exception(s"Failed to obtain Yanolja review: ${response.statusText} ${response.code}")
          }
        }
      }

  }
  def processYanoljaReview(
      result: YanoljaReviewContentResponse,
      config: YanoljaConfig,
      httpClient: HttpClient
  ): Future[YanoljaReviewContentResponse] = {
    val totalPages = Math.ceil(result.data.get.total.toDouble / config.reviewMaxPageSize).toInt
    if (totalPages > 1) {
      updateYanoljaReviews(result, config, totalPages,2)
    } else {
      Future.successful(result)
    }
  }
  def updateYanoljaReviews(
      result: YanoljaReviewContentResponse,
      config: YanoljaConfig,
      totalPages: Int,
      pageNum: Int
  )Future[YanoljaReviewContentResponse] = {
    val reviewContentData = result.data.get
    if (pageNum <= totalPages) {
      val request = getReviewContentRequest(reviewContentData.reviews.head.propertyId, config, pageNum)
      httpClient.send(request).flatMap { response =>
        if (response.code == 200) {
                    val rawBody: String = response.unsafeBody
           println(s"raw body 2 is $rawBody")
          decode[YanoljaReviewContentResponse](rawBody) match {
            case Right(res) =>
              logger.info(s"[DEBUG] Processed page $pageNum of $totalPages")
              result.addReviewData(res.data.get.reviews)
              updateYanoljaReviews(result, config, totalPages, pageNum + 1)
            case Left(e) =>
              logger.error(s"error when parsing raw Yanolja hotel review content response $rawBody", e)
              throw e
          }
        } else {
          logger.error(s"Failed to obtain Yanolja review: ${response.statusText} ${response.code}")
          throw new Exception(s"Failed to obtain Yanolja review: ${response.statusText} ${response.code}")
        }
      }
    } else {
      Future.successful(result)
    }
  }


-- using for loop
  def processYanoljaReview(
      result: YanoljaReviewContentResponse,
      config: YanoljaConfig,
      httpClient: HttpClient
  ): Future[YanoljaReviewContentResponse] = {
    val totalPages = Math.ceil(result.data.get.total.toDouble / config.reviewMaxPageSize).toInt
    if (totalPages > 1) {
      (2 to totalPages).foreach { pageNum =>
        val reviewContentData = result.data.get
        val request = getReviewContentRequest(reviewContentData.reviews.head.propertyId, config, pageNum)
        updateYanoljaReviews(result, request)
      }
    }
    Future.successful(result)
  }

  def updateYanoljaReviews(
      result: YanoljaReviewContentResponse,
      request: RequestT[Id, String, Nothing]
  ) : Future[YanoljaReviewContentResponse] = {
    httpClient.send(request).flatMap { response =>
      if (response.code == 200) {
        val rawBody: String = response.unsafeBody
        println(rawBody)
        decode[YanoljaReviewContentResponse](rawBody) match {
          case Right(res) =>
            result.addReviewData(res.data.get.reviews)
            Future.successful(result)
          case Left(e) =>
            logger.error(s"error when parsing raw Yanolja hotel review content response $rawBody", e)
            throw e
        }
      } else {
        logger.error(s"Failed to obtain Yanolja review: ${response.statusText} ${response.code}")
        throw new Exception(s"Failed to obtain Yanolja review: ${response.statusText} ${response.code}")
      }
    }
  }