# Mattermost plugin for Jenkins

Based on a fork of the Slack plugin:

https://github.com/jenkinsci/slack-plugin/

Which was a fork of the HipChat plugin:

https://github.com/jlewallen/jenkins-hipchat-plugin

Which was, in turn, a fork of the Campfire plugin.

Includes [Jenkins Pipeline](https://github.com/jenkinsci/workflow-plugin) support as of version 2.0:

```
slackSend color: 'good', message: 'Message from Jenkins Pipeline'
```

# Jenkins Instructions

1. Set up a Mattermost server
2. Configure an incoming webhook
3. Install this plugin on your Jenkins server
4. **Add it as a Post-build action** in your Jenkins job.

# Developer instructions

Install Maven and JDK.

Run unit tests

    mvn test

Create an HPI file to install in Jenkins (HPI file will be in `target/mattermost.hpi`).

    mvn package
