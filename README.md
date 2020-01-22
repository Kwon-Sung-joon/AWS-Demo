# Cloud AWS 프로젝트

### (목적)
* AWS의 EC2 인스턴스를 콘솔을 사용하지 않고 프로그램으로 자동 처리 

### 구현
* 로컬에 저장 된 자격 증명 파일로 새로운 IAM 유저 생성 및 정책 부여(AWS 모범 사례 참고)
* 생성한 IAM 유저로 EC2 인스턴스 제어(생성, 시작, 정지, 종료, 상태정보)
* IAM 유저의 사용이 끝난 후 해당 유저 삭제


### 수정
* IAM 유저 생성 후 10초 가량 대기 후 사용 가능 -> 로딩 페이지로 대체
* 인스턴스 연결을 위하여 보안그룹, 키페어 사용하여 인스턴스 생성

### 참고사이트
* [Java용 AWS SDK 개발자 가이드](https://docs.aws.amazon.com/ko_kr/sdk-for-java/v1/developer-guide/aws-sdk-java-dg.pdf)
* [EC2 Sample Code](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-ec2-instances.html)
* [IAM Sample Code](https://github.com/awsdocs/aws-doc-sdk-examples/tree/master/java/example_code/iam/src/main/java/aws/example/iam)
* [AWS 루트 계정](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/id_root-user.html)
* [인스턴스 연결 방법 (PuTTY)](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/putty.html)

### 선행조건
* [AWS 계정 생성](https://aws.com) 
* [EC2 인스턴스 생성](https://victorydntmd.tistory.com/61)
* [AWS CLI 설치](https://docs.aws.amazon.com/ko_kr/cli/latest/userguide/cli-chap-install.html)
* [AWS CLI 자격 증명 파일 설정](https://docs.aws.amazon.com/ko_kr/cli/latest/userguide/cli-configure-files.html)
* 생성한 자격 증명 파일에는 IAMFullAccess 권한 필요
* 인스턴스 연결 시 보안그룹 생성 및 키페어 생성 필요 