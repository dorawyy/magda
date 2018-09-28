import * as MarkdownIt from "markdown-it";

import { DatasetMessage } from "./model";
import { Record } from "@magda/typescript-common/dist/generated/registry/api";
import CEmailTplRender from "./CEmailTplRender";
import { Attachment } from "./SMTPMailer";

export enum Templates {
    Feedback = "feedback.mustache",
    Question = "question.mustache",
    Request = "request.mustache"
}

const md = new MarkdownIt({
    breaks: true
});

export interface RenderResult {
    renderedContent: string;
    attachments: Attachment[];
}

export default async function renderTemplate(
    tplRender: CEmailTplRender,
    templateFile: string,
    message: DatasetMessage,
    subject: string,
    externalUrl: string,
    dataset?: Record
): Promise<RenderResult> {
    const templateContext = {
        message: {
            ...message,
            html: md.render(message.message)
        },
        subject,
        dataset: dataset && {
            ...dataset.aspects["dcat-dataset-strings"],
            url: externalUrl + "/dataset/" + encodeURIComponent(dataset.id)
        }
    };

    const renderedContent = await tplRender.render(
        templateFile,
        templateContext
    );
    return { renderedContent, attachments: tplRender.attachments };
}
