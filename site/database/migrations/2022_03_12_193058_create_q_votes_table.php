<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateQVotesTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('q_votes', function (Blueprint $table) {
            $table->increments('Qv_id');
            $table->integer('Qt_id')->comment('Topic ID')->unsigned()->nullable();
            $table->integer('Qa_id')->comment('Answer ID')->unsigned()->nullable();
            $table->string('Qv_freetext', 350);




            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Qt_id')->references('Qt_id')->on('q_topic');
            $table->foreign('Qa_id')->references('Qa_id')->on('q_answers');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('q_votes');
    }
}
